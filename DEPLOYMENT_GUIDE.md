# 云聊 IM 服务器 - 部署指南

## 📦 编译完成状态

**编译时间**: 2025年10月18日  
**总模块数**: 57/57 模块  
**编译状态**: ✅ **100% 成功**  
**总编译时间**: 02:55 分钟

---

## 🎯 生成的部署包

### WAR 应用包 (4个)

| 应用名称 | WAR 包名 | 大小 | 路径 |
|---------|---------|------|------|
| **主 IM API 服务** | imapi.war | 117.18 MB | `im-parent/mianshi-im-api/target/` |
| **第三方推送服务** | third-push-socket-2.0.war | 90.87 MB | `im-parent/third-push/target/` |
| **消息推送服务** | message-push.war | 75.26 MB | `im-parent/message-push/target/` |
| **MP 服务器** | mp-server-socket-2.0.war | 103.38 MB | `im-parent/mp-server/target/` |

### JAR 库包 (1个)

| 库名称 | JAR 包名 | 路径 |
|--------|---------|------|
| **核心服务库** | mianshi-service-socket-2.0.jar | `im-parent/mianshi-service/target/` |

---

## 🚀 部署步骤

### 1. 环境要求

- **Java**: JDK 11 或更高版本 (推荐 JDK 11/17)
- **应用服务器**: Tomcat 9.x / Jetty / 或内嵌 Spring Boot
- **数据库**: MongoDB 3.x+
- **缓存**: Redis 5.x+
- **消息队列**: RocketMQ (可选)

### 2. 部署 WAR 包

#### 方式一: 使用 Tomcat 部署

```bash
# 1. 将 WAR 包复制到 Tomcat webapps 目录
cp im-parent/mianshi-im-api/target/imapi.war /path/to/tomcat/webapps/
cp im-parent/third-push/target/third-push-socket-2.0.war /path/to/tomcat/webapps/
cp im-parent/message-push/target/message-push.war /path/to/tomcat/webapps/
cp im-parent/mp-server/target/mp-server-socket-2.0.war /path/to/tomcat/webapps/

# 2. 启动 Tomcat
cd /path/to/tomcat/bin
./startup.sh  # Linux/Mac
# 或
startup.bat   # Windows
```

#### 方式二: 使用 Spring Boot 内嵌服务器运行

```bash
# 各个 WAR 包都内置了 Spring Boot,可以直接运行
java -jar imapi.war
java -jar third-push-socket-2.0.war
java -jar message-push.war
java -jar mp-server-socket-2.0.war
```

### 3. 配置文件说明

每个应用需要配置以下内容(在 `application.properties` 或 `application.yml` 中):

#### MongoDB 配置
```properties
spring.data.mongodb.uri=mongodb://localhost:27017/yunliao
spring.data.mongodb.database=yunliao
```

#### Redis 配置
```properties
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=
```

#### 应用端口配置
```properties
# 建议端口分配
# imapi.war - 8080
# third-push - 8081
# message-push - 8082
# mp-server - 8083
server.port=8080
```

---

## 🔧 关键配置项

### 1. 推送服务配置

在 `application.properties` 中配置各厂商推送服务:

```properties
# 小米推送
push.xiaomi.app.id=YOUR_XIAOMI_APP_ID
push.xiaomi.app.key=YOUR_XIAOMI_APP_KEY
push.xiaomi.app.secret=YOUR_XIAOMI_APP_SECRET

# OPPO 推送
push.oppo.app.key=YOUR_OPPO_APP_KEY
push.oppo.master.secret=YOUR_OPPO_MASTER_SECRET

# 华为推送
push.huawei.app.id=YOUR_HUAWEI_APP_ID
push.huawei.app.secret=YOUR_HUAWEI_APP_SECRET

# 极光推送
push.jpush.app.key=YOUR_JPUSH_APP_KEY
push.jpush.master.secret=YOUR_JPUSH_MASTER_SECRET

# VIVO 推送 (当前已禁用,需要 SDK v2.2+ 支持)
# push.vivo.app.id=YOUR_VIVO_APP_ID
# push.vivo.app.key=YOUR_VIVO_APP_KEY
# push.vivo.app.secret=YOUR_VIVO_APP_SECRET
```

### 2. IM 核心配置

```properties
# t-io 配置
im.server.host=0.0.0.0
im.server.port=9326
im.server.heartbeat.timeout=120000

# 文件上传路径
file.upload.path=/data/yunliao/upload

# 日志路径
logging.file.path=/data/yunliao/logs
```

---

## 📋 部署验证

### 1. 检查应用启动

```bash
# 查看日志确认启动成功
tail -f /path/to/logs/application.log

# 或检查 Tomcat 日志
tail -f /path/to/tomcat/logs/catalina.out
```

### 2. 健康检查端点

访问以下 URL 检查服务状态:

- **主 API**: `http://localhost:8080/actuator/health`
- **推送服务**: `http://localhost:8081/actuator/health`
- **消息推送**: `http://localhost:8082/actuator/health`
- **MP 服务**: `http://localhost:8083/actuator/health`

### 3. API 文档

访问 Swagger UI 查看 API 文档:

- `http://localhost:8080/swagger-ui.html`

---

## ⚠️ 重要提示

### VIVO 推送服务状态

**当前状态**: ⚠️ **已禁用**

**原因**: VIVO SDK v1.0 与代码使用的 v2.2 API 不兼容

**影响**: 
- VIVO 手机用户将无法收到推送通知
- 其他推送服务(小米、OPPO、华为、极光、魅族)正常工作
- 应用可正常启动和运行,不影响核心功能

**解决方案**:
1. 获取 VIVO Push SDK v2.2 或更高版本
2. 替换 `im-comm-modules/push-services-module/lib/vPush_SDK_Server.jar`
3. 恢复 `VIVOPushService.java` 中被注释的代码
4. 重新编译部署

---

## 🐛 故障排查

### 常见问题

1. **MongoDB 连接失败**
   ```
   检查 MongoDB 是否启动
   检查防火墙设置
   确认连接字符串正确
   ```

2. **Redis 连接失败**
   ```
   检查 Redis 是否启动
   确认 Redis 密码配置
   检查端口是否被占用
   ```

3. **端口被占用**
   ```bash
   # Windows
   netstat -ano | findstr :8080
   
   # Linux/Mac
   lsof -i :8080
   ```

4. **内存不足**
   ```bash
   # 增加 JVM 内存
   java -Xms512m -Xmx2048m -jar imapi.war
   ```

---

## 📊 性能优化建议

### JVM 参数优化

```bash
java -server \
  -Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/data/logs/heap_dump.hprof \
  -jar imapi.war
```

### MongoDB 优化

- 为常用查询字段建立索引
- 启用 MongoDB 分片 (高并发场景)
- 配置合适的连接池大小

### Redis 优化

- 使用 Redis Cluster (高可用)
- 配置持久化策略
- 设置合理的内存淘汰策略

---

## 📞 技术支持

如遇到部署问题,请检查:

1. 编译日志: `mvn clean package -DskipTests > build.log 2>&1`
2. 应用日志: `/data/yunliao/logs/`
3. 系统日志: `/var/log/syslog` 或 Windows 事件查看器

---

## 📝 版本信息

- **项目版本**: socket-2.0 / 3.0-SNAPSHOT
- **Spring Boot**: 2.2.5.RELEASE
- **JDK**: 11+ (编译目标)
- **t-io**: 3.6.2
- **编译日期**: 2025-10-18

---

## ✅ 编译成功确认

```
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:55 min
[INFO] Finished at: 2025-10-18T11:46:40+07:00
[INFO] ------------------------------------------------------------------------
```

**所有 57 个模块编译成功,项目已就绪部署!** 🎉
