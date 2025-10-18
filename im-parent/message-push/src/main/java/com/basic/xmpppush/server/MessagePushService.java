package com.basic.xmpppush.server;

import com.basic.im.comm.model.MessageBean;
import com.basic.im.friends.service.FriendsManager;
import com.basic.im.friends.service.FriendsRedisRepository;
import com.basic.im.push.server.AbstractMessagePushService;
import com.basic.im.user.service.UserRedisService;
import com.basic.im.utils.SKBeanUtils;
import com.chat.imclient.BaseClientHandler;
import com.chat.imclient.BaseClientListener;
import com.chat.imclient.BaseIMClient;
import com.chat.imserver.common.message.AuthMessage;
import com.chat.imserver.common.message.ChatMessage;
import com.chat.imserver.common.message.MessageHead;
import com.chat.imserver.common.packets.ChatType;
import com.chat.imserver.common.utils.StringUtils;
import com.basic.utils.DateUtil;
import com.basic.utils.StringUtil;
import com.basic.xmpppush.config.IMConfig;
import com.basic.xmpppush.prometheus.CustomIndicator;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.tio.core.ChannelContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class MessagePushService extends AbstractMessagePushService {


	private Map<String,Object> messageMap=new ConcurrentHashMap<>();


	@Autowired(required = false)
	protected IMConfig imConfig;


	@Autowired(required = false)
	protected FriendsRedisRepository friendsRedisRepository;

	@Autowired(required = false)
	protected FriendsManager friendsManager;


	@Autowired(required = false)
	protected UserRedisService userRedisService;



	private List<String> sysUserList=null;
	private synchronized List<String> getUserList(){
		if(null!=sysUserList)
			return sysUserList;


		sysUserList= Collections.synchronizedList(new ArrayList<String>());
		for (String string : systemAdminMap.keySet()) {
			sysUserList.add(string);
		}
		return sysUserList;
	}


	// 新的队列
	public static  ConcurrentLinkedQueue<ChatMessage> queue = new ConcurrentLinkedQueue<>();

	@Autowired(required = false)
	private MeterRegistry registry;

	@Override
	public void afterPropertiesSet() throws Exception {
		initThread();
	}




	public  void initThread(){
		ImPushQueueThread work;
		IMClient client=null;
		/*if(!StringUtil.isEmpty(imConfig.getUserIds())){
			String[] userIdList = imConfig.getUserIds().split(",");
			for(int i=0;i<getUserList().size();i++) {
				if (getUserList().get(i) == "10000") {
					continue;
				}
			}
		}*/

		List<String> userList = getUserList();

		if(!userList.isEmpty()) {
			for (int i = 0; i < userList.size(); i++) {
				if (userList.get(i) == "10000") {
					continue;
				}

				try {
					client = getIMClient(userList.get(i));
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
				work = new ImPushQueueThread(client);
				work.start();
			}
		}else {
			try {
				client = getIMClient("10005");
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			work = new ImPushQueueThread(client);
			work.start();
		}


	}

	public ChatMessage bulidChatMessage(MessageBean messageBean){
		ChatMessage message = null;
		MessageHead messageHead = null;
		try {
			message = new ChatMessage();
			messageHead = new MessageHead();
			if(!StringUtil.isEmpty(messageBean.getTo())){
				messageHead.setTo(messageBean.getTo());
			}else{
				messageHead.setTo(messageBean.getToUserId());
			}

			byte chatType= ChatType.CHAT;
			if(1==messageBean.getMsgType()) {
				chatType=ChatType.GROUPCHAT;
				messageHead.setTo(messageBean.getRoomJid());
				//messageBean.setToUserId(messageBean.getRoomJid());
			}else if(2==messageBean.getMsgType()) {
				chatType=ChatType.ALL;
			}

			messageHead.setChatType(chatType);
			if(null!=messageBean.getMessageId()) {
				messageHead.setMessageId(messageBean.getMessageId());
			} else {
				messageHead.setMessageId(StringUtils.newStanzaId());
			}
			if(null!=messageBean.getContent()) {
				message.setContent(messageBean.getContent().toString());
			}
			message.setFromUserId(messageBean.getFromUserId());
			message.setFromUserName(messageBean.getFromUserName());
			message.setToUserId(messageBean.getToUserId());
			message.setToUserName(messageBean.getToUserName());
			message.setType((short)messageBean.getType());
			message.setSubType(messageBean.getSubType());
			//messageBean.setTimeSend(timeSend);
			if (null != messageBean.getTimeSend()) {
				message.setTimeSend(Long.parseLong(messageBean.getTimeSend().toString()));
			}

			if (null != messageBean.getObjectId()) {
				message.setObjectId(messageBean.getObjectId().toString());
			}
			if (null != messageBean.getFileName()) {
				message.setFileName(messageBean.getFileName());
			}
			if(0!=messageBean.getFileSize()){
				message.setFileSize(messageBean.getFileSize());
			}

			if(0!=messageBean.getTimeLen()){
				message.setFileTime(messageBean.getTimeLen());
			}

			message.setOther(messageBean.getOther());


			message.setMessageHead(messageHead);

			if(isNeedSeqNo(message.getType())) {
				message.setSeqNo(-1);
			}else if(ChatType.GROUPCHAT == messageHead.getChatType()&&(9==(message.getType()/100)||806==message.getType())){
				message.setSeqNo(-1);
			}


			/**
			 * 回复给访客的消息
			 */
			if( null != messageBean.getSrvId() && 0 != messageBean.getSrvId()){
				message.setSrvId(messageBean.getSrvId());
			}

		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return message;
	}


	private void setTimeSend(com.basic.im.comm.model.MessageBean messageBean){
		if(null==messageBean.getTimeSend()){
			messageBean.setTimeSend((System.currentTimeMillis()));
		}
	}

	@Override
	public void onMessage(MessageBean messageBean) {
		ChatMessage message=null;

		String body=null;
		try {
			setTimeSend(messageBean);
			if(2==messageBean.getMsgType()){
				sendBroadCast(messageBean);
				return;
			}
			message=bulidChatMessage(messageBean);
			if(null==message) {
				return;
			}
			if(null!=registry) {
				//累加到Prometheus
				CustomIndicator.singleMessageSum(registry).increment();
			}
			queue.offer(message);
		} catch (Exception e) {
			log.error("=== "+body+" ===> "+e.getMessage());
		}

	}

	/**
	 * 发送单聊消息
	 * @param body
	 */
	@Override
	public void send(MessageBean body){
		try {

			// 把消息丢进queue队列中

		} catch (Exception e) {
			e.printStackTrace();
			log.error("放进队列失败! ==="+e.getMessage());
		}
	}



	/**
	 * 发送群组消息
	 * @param body
	 * @throws Exception
	 */
	@Override
	public void sendGroup(MessageBean body)  {
        setTimeSend(body);
        ChatMessage message=bulidChatMessage(body);
        if(null!=message){
        	return;
		}
		try {
			if(null!=registry) {
				//累加到Prometheus
				CustomIndicator.groupMessageSum(registry).increment();
			}
			// 把消息丢进queue队列中
			queue.offer(message);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("放进队列失败!" + (null!=message?message.toString():""));
		}

	}

	@Override
	public void sendBroadCast(MessageBean messageBean){
		List<Integer> list;
		/*list = friendsRedisRepository.getFriendsUserIdsList(Integer.valueOf(messageBean.getFromUserId()));
		if(list.size()==0){
			list =friendsManager.queryFansId(Integer.valueOf(messageBean.getFromUserId()));
			//SKBeanUtils.getRedisService().saveFriendsUserIdsList(Integer.valueOf(messageBean.getFromUserId()),list);
		}*/

		if(Integer.valueOf(messageBean.getFromUserId())>10200){
			list = friendsRedisRepository.getFriendsUserIdsList(Integer.valueOf(messageBean.getFromUserId()));
			if(list.size()==0){
				list = friendsManager.queryFollowId(Integer.valueOf(messageBean.getFromUserId()));
				//SKBeanUtils.getRedisService().saveFriendsUserIdsList(Integer.valueOf(body.getFromUserId()),list);
			}
		}else{
			//list = userRedisService.getNoSystemNumUserIds();
			Query query = new Query().addCriteria(Criteria.where("_id").gt(10200));
			list=SKBeanUtils.getDatastore().findDistinct(query,"_id","user",Integer.class);
				//SKBeanUtils.getRedisService().saveNoSystemNumUserIds(list);


		}
		for(Integer userId:list){
			ChatMessage message=null;
			MessageHead messageHead=null;
			try {
				message=new ChatMessage();
				messageHead=new MessageHead();
				messageHead.setFrom(messageBean.getFromUserId());
				messageHead.setTo(userId.toString());
				messageHead.setChatType(ChatType.CHAT);
				message.setFromUserId(messageBean.getFromUserId());
				message.setFromUserName(messageBean.getFromUserName());
				message.setToUserId(userId.toString());
				message.setToUserName(messageBean.getToUserName());
				message.setType((short)messageBean.getType());
				message.setSeqNo(-1);

				message.setTimeSend(System.currentTimeMillis());
				message.setContent(messageBean.getContent().toString());
				if(null!=messageBean.getMessageId()) {
					messageHead.setMessageId(messageBean.getMessageId());
				} else {
					messageHead.setMessageId(StringUtils.newStanzaId());
				}
				message.setMessageHead(messageHead);

				// 把消息丢进queue队列中
				queue.offer(message);
				Thread.sleep(20);
			} catch (Exception e) {
				log.error(e.getMessage(),e);


			}

		}
	}


	private IMClient getIMClient(String userId) {
		IMClient client=new IMClient();
		client.setUserId(userId);

		client.setPingTime(imConfig.getPingTime());

		BaseClientHandler clientHandler=new BaseClientHandler() {


			@Override
			public void handlerReceipt(String messageId) {
				System.out.println("handlerReceipt ===> "+messageId);
				messageMap.remove(messageId);
			}
		};
		clientHandler.setImClient(client);
		BaseClientListener clientListener=new BaseClientListener() {

			@Override
			public AuthMessage authUserMessage(ChannelContext channelContext, BaseIMClient client) {
				MessageHead messageHead=new MessageHead();

				messageHead.setChatType(ChatType.CHAT);
				channelContext.userid=userId;
				messageHead.setFrom(userId+"/Server");
				messageHead.setTo("service");
				messageHead.setMessageId(UUID.randomUUID().toString().replaceAll("-", ""));

				AuthMessage authMessage=new AuthMessage();
				authMessage.setToken(imConfig.getServerToken());
				authMessage.setPassword("");
				authMessage.setDeviceId("1111");
				authMessage.setVersion((short)4);
				authMessage.setApiKey("1");
				authMessage.setAppName("1");
				authMessage.setCompanyName("1");
				authMessage.setSecret("1");
				authMessage.setMessageHead(messageHead);
				return authMessage;
			}
		};
		clientListener.setImClient(client);
		client.initIMClient(imConfig.getHost(),imConfig.getPort(),clientHandler,clientListener);

		return client;
	}



	/**
	 * 推送Queue队列中的消息
	 * @throws InterruptedException
	 */
	public void runQueuePush(IMClient client)
			throws Exception{
		ChatMessage message=queue.poll();

		if(message==null){
			return;
		}
		try {
			if(null==client) {
				Thread.sleep(500);
			}
			if(null != message.getMessageHead()){
                message.getMessageHead().setFrom(client.getUserId()+"/Server");
            }
			System.out.println("--------runQueuePush----------"+client.getUserId());
			ChatMessageVo messageVo=new ChatMessageVo();
			messageVo.setCreateTime(DateUtil.currentTimeSeconds());
			messageVo.setMessage(message);
			client.sendMessage(message);
			if(null != message.getMessageHead()) {
				messageMap.put(message.getMessageHead().getMessageId(), messageVo);
			}
			log.info("系统推送成功： to {},",message.getToUserId());
		}  catch (Exception e) {
			queue.offer(message);
			log.error(e.getMessage(),e);
		}
	}



	/**
	 *
	 * @Description: TODO(在线程中消费队列中的消息)
	 * @author Administrator
	 * @date 2018年12月26日 上午11:26:22
	 * @version V1.0
	 */
	public class ImPushQueueThread extends Thread {
		private IMClient client=null;

		public ImPushQueueThread() {}

		public ImPushQueueThread(IMClient client) {
			this.client=client;
		}

		@Override
		public void run() {
			while (true) {
				if(!queue.isEmpty()){
					try {
						runQueuePush(client);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}else{
					try {
						Thread.sleep(100);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

	}


	@Getter
	@Setter
	public class ChatMessageVo {
		private long createTime;
		private ChatMessage message;
	}


	/**
	 * 定时重发
	 * @throws InterruptedException
	 */
	/*public void timer(){
		new Thread(new Runnable() {

			@Override
			public void run() {
				while(true){
					Long startTime=System.currentTimeMillis();
					if(messageMap.size()>10){
						log.info("开始时间"+DateUtil.currentTimeSeconds()+"   map大小   "+messageMap.size());
					}
					Set<Map.Entry<String, ChatMessageVo>> set=messageMap.entrySet();
					for (Map.Entry<String, ChatMessageVo> entry : set) {
						ChatMessageVo messageVo=entry.getValue();
						if(messageVo!=null){
							if(DateUtil.currentTimeSeconds()-messageVo.getCreateTime()>=30){
								queue.offer(messageVo.getMessage());
							}
						}else {
							return;
						}
					}

					Long endTime=System.currentTimeMillis();
					if((endTime-startTime)>1000){
						log.info("执行map所需要的时间========"+(endTime-startTime));
					}
					try {
						Thread.sleep(30000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}

		}).start();

	}*/
}
