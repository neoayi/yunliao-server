# 云聊 IM 服务器配置模板

## 📋 配置文件说明

部署前请根据实际环境修改以下配置文件:
- `deploy/config/application.properties` - 主API配置
- `deploy/config/application-local.properties` - 本地环境配置

---

## 🔧 核心配置项

### 1. MongoDB 配置 (必改)
```properties
# MongoDB 连接地址
mongoconfig.uri=mongodb://127.0.0.1:27017

# 数据库名称
mongoconfig.dbName=imapi
mongoconfig.roomDbName=imRoom

# 认证信息 (如果MongoDB开启了认证)
mongoconfig.username=your_username
mongoconfig.password=your_password

# 连接超时设置
mongoconfig.connectTimeout=20000
mongoconfig.socketTimeout=20000
mongoconfig.maxWaitTime=20000
```

**生产环境示例:**
```properties
mongoconfig.uri=mongodb://mongodb.production.com:27017
mongoconfig.dbName=imapi_prod
mongoconfig.roomDbName=imRoom_prod
mongoconfig.username=im_user
mongoconfig.password=StrongP@ssw0rd!2025
```

---

### 2. Redis 配置 (必改)
```properties
# Redis 服务器地址
spring.redis.host=127.0.0.1
spring.redis.port=6379

# Redis 密码 (如果设置了密码)
spring.redis.password=

# 连接池配置
spring.redis.jedis.pool.max-active=100
spring.redis.jedis.pool.max-idle=20
spring.redis.jedis.pool.max-wait=3000
```

**生产环境示例:**
```properties
spring.redis.host=redis.production.com
spring.redis.port=6379
spring.redis.password=Redis@2025!Secure
spring.redis.database=0
spring.redis.timeout=5000
```

---

### 3. 服务端口配置
```properties
# 主API服务端口
server.port=8080

# 其他服务端口建议:
# - third-push: 8081
# - message-push: 8082
# - mp-server: 8083
```

---

### 4. 文件上传配置
```properties
# 文件上传域名 (修改为实际的文件服务器地址)
appConfig.uploadDomain=http://127.0.0.1:8088

# 文件上传路径
file.upload.path=/data/uploads
```

**生产环境示例:**
```properties
appConfig.uploadDomain=https://cdn.yourdomain.com
file.upload.path=/opt/yunliao/uploads
```

---

### 5. XMPP 配置 (即时通讯核心)
```properties
# XMPP 服务器地址
xmppConfig.host=127.0.0.1
xmppConfig.serverName=im.server.com
xmppConfig.port=5666

# XMPP 推送账号
xmppConfig.username=10005
xmppConfig.password=10005

# XMPP MongoDB 配置
xmppConfig.dbUri=mongodb://127.0.0.1:27017
xmppConfig.dbName=tigase
```

**生产环境示例:**
```properties
xmppConfig.host=xmpp.production.com
xmppConfig.serverName=im.yourdomain.com
xmppConfig.port=5666
xmppConfig.username=admin_push
xmppConfig.password=SecureXMPP@2025
xmppConfig.dbUri=mongodb://mongodb.production.com:27017
xmppConfig.dbName=tigase_prod
```

---

### 6. RocketMQ 配置 (消息队列)
```properties
# RocketMQ 服务器地址
rocketmq.name-server=127.0.0.1:9876

# 生产者组
rocketmq.producer.group=group-xmpppush
rocketmq.producer.send-message-timeout=30000
```

**生产环境示例:**
```properties
rocketmq.name-server=rocketmq1.prod.com:9876;rocketmq2.prod.com:9876
rocketmq.producer.group=group-xmpppush-prod
rocketmq.producer.send-message-timeout=30000
```

---

### 7. 短信服务配置 (可选)
```properties
# 是否开启短信验证码
smsConfig.openSMS=1

# 阿里云短信配置
smsConfig.product=Dysmsapi
smsConfig.domain=dysmsapi.aliyuncs.com
smsConfig.accesskeyid=YOUR_ACCESS_KEY_ID
smsConfig.accesskeysecret=YOUR_ACCESS_KEY_SECRET
smsConfig.signname=您的签名
smsConfig.chinase_templetecode=SMS_TEMPLATE_CODE
smsConfig.english_templetecode=SMS_TEMPLATE_CODE_EN
```

---

### 8. 推送服务配置 (可选)

#### 小米推送
```properties
xiaomi.push.appSecret=YOUR_XIAOMI_APP_SECRET
xiaomi.push.packageName=com.yourapp.package
```

#### OPPO 推送
```properties
oppo.push.appKey=YOUR_OPPO_APP_KEY
oppo.push.masterSecret=YOUR_OPPO_MASTER_SECRET
```

#### 华为推送
```properties
huawei.push.appId=YOUR_HUAWEI_APP_ID
huawei.push.appSecret=YOUR_HUAWEI_APP_SECRET
```

#### JPush (极光推送)
```properties
jpush.appKey=YOUR_JPUSH_APP_KEY
jpush.masterSecret=YOUR_JPUSH_MASTER_SECRET
```

**注意:** VIVO 推送已禁用 (SDK版本不兼容)

---

### 9. Tomcat 配置优化
```properties
# 最大连接数
server.tomcat.max-connections=3000

# 最大线程数
server.tomcat.max-threads=1000

# 最大POST请求大小 (1MB)
server.tomcat.max-http-post-size=1048576

# 请求头最大大小 (1MB)
server.max-http-header-size=1048576
```

---

### 10. HTTPS 配置 (生产环境推荐)
```properties
# 开启HTTPS
server.openHttps=true

# SSL证书配置
server.ssl.key-store=classpath:imapi.p12
server.ssl.key-store-password=YOUR_KEYSTORE_PASSWORD
server.ssl.key-store-type=PKCS12

# HTTP端口 (可选,用于HTTP到HTTPS重定向)
http.port=8080
```

---

## 📝 配置检查清单

部署前请确认以下配置已修改:

- [ ] MongoDB 连接地址和认证信息
- [ ] Redis 连接地址和密码
- [ ] 服务端口号 (避免冲突)
- [ ] 文件上传域名和路径
- [ ] XMPP 服务器地址和认证
- [ ] RocketMQ 服务器地址
- [ ] 短信服务配置 (如需要)
- [ ] 推送服务配置 (如需要)
- [ ] HTTPS 证书配置 (生产环境)
- [ ] 日志路径和级别

---

## 🚀 快速配置向导

### 最小化配置 (本地开发)
只需修改以下3项即可启动:
1. `mongoconfig.uri` - MongoDB地址
2. `server.port` - 服务端口
3. `xmppConfig.host` - XMPP服务器地址

### 生产环境配置
除最小化配置外,还需配置:
1. Redis 连接和密码
2. RocketMQ 地址
3. 文件上传域名
4. HTTPS 证书
5. 推送服务密钥
6. 短信服务配置

---

## ⚠️ 安全建议

1. **数据库密码**: 使用强密码,定期更换
2. **Redis密码**: 生产环境必须设置密码
3. **API密钥**: 妥善保管各种推送服务的密钥
4. **HTTPS**: 生产环境强烈建议启用HTTPS
5. **访问控制**: 配置防火墙,限制数据库访问IP
6. **日志脱敏**: 避免在日志中输出敏感信息

---

## 📖 相关文档

- 完整部署指南: `DEPLOYMENT_GUIDE.md`
- 启动脚本: `deploy.bat`
- 配置示例: `im-parent/mianshi-im-api/src/main/resources/`

---

## 🆘 配置问题排查

### MongoDB 连接失败
- 检查 MongoDB 服务是否启动
- 验证连接地址和端口
- 确认认证信息正确
- 检查防火墙规则

### Redis 连接失败
- 检查 Redis 服务是否启动
- 验证密码是否正确
- 确认 Redis 绑定了正确的IP
- 检查 Redis 最大连接数配置

### 推送服务不工作
- 确认对应的推送服务配置正确
- 检查 appKey 和 secret 是否有效
- 查看日志中的推送错误信息
- VIVO 推送已禁用,使用其他推送服务

### XMPP 连接问题
- 确认 Tigase/Openfire 服务器已启动
- 验证 XMPP 服务器地址和端口
- 检查推送账号是否创建
- 查看 XMPP 服务器日志

---

**版本信息:**
- 项目版本: socket-2.0
- Spring Boot: 2.2.5.RELEASE
- Java 运行时: JDK 11+
- 文档更新: 2025-10-18
