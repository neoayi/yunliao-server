package com.basic.mianshi.rocketmq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.basic.im.admin.dao.KeywordDAO;
import com.basic.im.admin.entity.KeywordDenyRecord;
import com.basic.im.admin.service.impl.AdminManagerImpl;
import com.basic.im.comm.model.MessageBean;
import com.basic.im.comm.utils.StringUtil;
import com.basic.im.config.AppConfig;
import com.basic.im.message.MessageService;
import com.basic.im.message.MessageType;
import com.basic.im.room.entity.Room;
import com.basic.im.room.service.impl.RoomManagerImplForIM;
import com.basic.im.user.service.UserCoreService;
import com.basic.utils.DateUtil;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RocketMQMessageListener(topic = "keywordMessage", consumerGroup = "my-consumer-keywordMessage")
public class KeywordConsumer  implements RocketMQListener<String>{

	    private static final Logger log = LoggerFactory.getLogger(KeywordConsumer.class);


		@Autowired
		private AppConfig appConfig;


		@Lazy
		@Autowired
		private KeywordDAO keywordDAO;
		@Lazy
		@Autowired
		protected UserCoreService userCoreService;

		@Lazy
		@Autowired
		private AdminManagerImpl adminManager;

		@Autowired
		@Lazy
		private RoomManagerImplForIM roomManager;

		@Autowired
		@Lazy
		private MessageService messageService;




		//单聊普通敏感词预警数量
		//private  int chatWarningKeywordNum = appConfig.getChatWarningKeywordNum();

		//单聊否词预警数量
		//private  int chatWarningnotKeywordNum = appConfig.getChatWarningNotwordNum();

		//群聊普通敏感词预警数量
		//private  int groupWarningKeywordNum = appConfig.getGroupWarningKeywordNum();

		//群聊否词预警数量
		//private  int groupWarningNotKeywordNum = appConfig.getGroupWarningNotwordNum();

		@Override
		public void onMessage(String message) {

			KeywordDenyRecord keywordDenyRecord = new KeywordDenyRecord();

			JSONObject messageObj = JSON.parseObject(message);
			//系统消息不做处理
			if( 10000 == messageObj.getInteger("fromUserId") || null != messageObj.getShort("imSys") ){
				return;
			}
			log.info("==========keywordDenyRecord============>>>>  "+JSON.toJSONString(keywordDenyRecord) );

			keywordDenyRecord.setMsgContent(messageObj.getString("content"));
			keywordDenyRecord.setFromUserId(messageObj.getInteger("fromUserId"));
			keywordDenyRecord.setFromUserName(messageObj.getString("fromUserName"));
			keywordDenyRecord.setCreateTime(System.currentTimeMillis());
            if(null != messageObj.getShort("chatType")){
                keywordDenyRecord.setChatType(1 == messageObj.getShort("chatType") ? (short) 1 : (short) 2);
            }else {
                keywordDenyRecord.setChatType(1 == messageObj.getShort("msgType") ? (short) 2 : (short) 1);
            }

			keywordDenyRecord.setMessageId(messageObj.getString("messageId"));
			keywordDenyRecord.setKeyword(messageObj.getString("keyword"));
			keywordDenyRecord.setKeywordType(messageObj.getShort("keywordType"));
			if (keywordDenyRecord.getChatType() == 2) { //群聊
				keywordDenyRecord.setRoomJid(messageObj.getString("toUserId"));
			} else {
				keywordDenyRecord.setToUserId(messageObj.getInteger("toUserId"));
			}
			keywordDenyRecord.setToUserName(messageObj.getString("toUserName"));

			try {
				//该消息id 已存在拦截记录，则不继续执行
				KeywordDenyRecord keywordDenyRecord1 = keywordDAO.findOne(KeywordDenyRecord.class, "messageId", keywordDenyRecord.getMessageId());
				if (null != keywordDenyRecord1) {
					return;
				}
				if(2==keywordDenyRecord.getChatType() && StringUtil.isEmpty(keywordDenyRecord.getToUserName())){
					Room room  = roomManager.getRoomByJid(keywordDenyRecord.getRoomJid());
					if(null!=room){
						keywordDenyRecord.setToUserName(room.getName());
					}
				}else{
					// 协议中无toUserName自行获取
					if(StringUtil.isEmpty(messageObj.getString("toUserName"))){
						keywordDenyRecord.setToUserName(userCoreService.getNickName(keywordDenyRecord.getToUserId()));
					}else{
						keywordDenyRecord.setToUserName(messageObj.getString("toUserName"));
					}
				}

				saveKeywordDenyRecord(keywordDenyRecord);

				String noticeTxt = (1 != keywordDenyRecord.getKeywordType() ? "敏感词截获":"否词拦截")+"通知:"+
						keywordDenyRecord.getFromUserName()+"("+keywordDenyRecord.getFromUserId()+")"+
						( 1==keywordDenyRecord.getChatType()  ? "给用户"+keywordDenyRecord.getToUserName()+"("+keywordDenyRecord.getToUserId()+")" :
								"给群组"+keywordDenyRecord.getToUserName()+"("+keywordDenyRecord.getRoomJid()+")" )
						+"发送了一条消息,含"+ (1 != keywordDenyRecord.getKeywordType() ? "敏感词":"否词")+" “"+keywordDenyRecord.getKeyword()+
						"” : "+keywordDenyRecord.getMsgContent();

				//通知管理后台默认好友
				noticeAdminDefultFriends(noticeTxt);

				sendRiskWarningMsgBegin(keywordDenyRecord);

			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		/**
		 * @Description:
		 */
		public void saveKeywordDenyRecord(KeywordDenyRecord keywordDenyRecord) {
			try {

				if (!StringUtil.isEmpty(keywordDenyRecord.getMessageId())) {
					//保存拦截记录
					keywordDAO.getDatastore().save(keywordDenyRecord);
				}

			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

		}


		/**
		 * @Description: TODO(根据用户关键词拦截数量, 做封号操作)
		 */
		public void baseKeywordDenyRecordCountCloseUserAccount(int userId) {
			long denyRecordCount = keywordDAO.count("fromUserId", userId, "keywordDenyRecord");
		}


		/**
		 * 通知管理后台设置的默认好友用户
		 */
		public void noticeAdminDefultFriends(String keywordMsgContent) {

			List<Integer> defFriendUserList = userCoreService.queryDefFriendUserIdList();

			if (null != defFriendUserList && 0 < defFriendUserList.size()) {
				for (Integer defUserId : defFriendUserList) {
					adminManager.sendMsgToUser(defUserId, 1, keywordMsgContent);
				}
			}

		}

		/**
		 * 发送风险提示消息
		 */
		public void sendRiskWarningMsgBegin (KeywordDenyRecord keywordDenyRecord) {

			long denyRecordCount =keywordDAO.queryKeywordDenyRecordCountByType(keywordDenyRecord.getFromUserId(),keywordDenyRecord.getKeywordType(),keywordDenyRecord.getChatType());

			if(1==keywordDenyRecord.getKeywordType()){ //否词
				if(1==keywordDenyRecord.getChatType()&&denyRecordCount>=appConfig.getChatWarningNotwordNum()){
					/**
					 * 发消息提示当前聊天好友
					 */
					sendsendRiskWarningMsg(keywordDenyRecord);
				}else if(2==keywordDenyRecord.getChatType()&&denyRecordCount>=appConfig.getGroupWarningNotwordNum()) {
					/**
					 * 发消息提示当前聊天群组
					 */
					sendsendRiskWarningMsg(keywordDenyRecord);
				}

			}else {
				/**
				 * 普通敏感词
				 */
				if(1==keywordDenyRecord.getChatType()&&denyRecordCount>=appConfig.getChatWarningKeywordNum()){
					/**
					 * 发消息提示当前聊天好友
					 */
					sendsendRiskWarningMsg(keywordDenyRecord);
				}else if(2==keywordDenyRecord.getChatType()&&denyRecordCount>=appConfig.getGroupWarningKeywordNum())  {
					/**
					 * 发消息提示当前聊天群组
					 */
					sendsendRiskWarningMsg(keywordDenyRecord);
				}
			}


		}

		public void sendsendRiskWarningMsg(KeywordDenyRecord keywordDenyRecord){

			MessageBean messageBean=new MessageBean();
			messageBean.setContent("平台识别到用户 "+keywordDenyRecord.getFromUserName()+" 在发送一些包含违规内容的消息,请勿相信，谨防上当受骗");
			messageBean.setFromUserId(keywordDenyRecord.getFromUserId() + "");
			messageBean.setToUserId(keywordDenyRecord.getToUserId()+"");
			messageBean.setFromUserName(keywordDenyRecord.getFromUserName());
			messageBean.setType(MessageType.sensitiveWordsNotice);
			messageBean.setMessageId(com.basic.utils.StringUtil.randomUUID());
			if(1==keywordDenyRecord.getChatType()) {
				messageBean.setToUserId(keywordDenyRecord.getToUserId() + "");
				messageBean.setMsgType(0);// 单聊
				messageService.send(messageBean);
			}else {
				messageBean.setMsgType(1);// 群聊
				messageBean.setObjectId(keywordDenyRecord.getRoomJid());
				messageService.sendMsgToGroupByJid(keywordDenyRecord.getRoomJid(),messageBean);
			}
		}

}
