@echo off
REM ===================================================================
REM 云聊 IM 服务器 - Windows 快速部署脚本
REM ===================================================================
echo.
echo ========================================
echo   云聊 IM 服务器 - 快速部署
echo ========================================
echo.

REM 设置变量
set PROJECT_DIR=%~dp0
set TARGET_DIR=%PROJECT_DIR%deploy
set TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

echo [1/5] 创建部署目录...
if not exist "%TARGET_DIR%" mkdir "%TARGET_DIR%"
if not exist "%TARGET_DIR%\backup" mkdir "%TARGET_DIR%\backup"
if not exist "%TARGET_DIR%\logs" mkdir "%TARGET_DIR%\logs"
if not exist "%TARGET_DIR%\config" mkdir "%TARGET_DIR%\config"

echo [2/5] 复制 WAR 包...
xcopy /Y "%PROJECT_DIR%im-parent\mianshi-im-api\target\imapi.war" "%TARGET_DIR%\" 
xcopy /Y "%PROJECT_DIR%im-parent\third-push\target\third-push-socket-2.0.war" "%TARGET_DIR%\"
xcopy /Y "%PROJECT_DIR%im-parent\message-push\target\message-push.war" "%TARGET_DIR%\"
xcopy /Y "%PROJECT_DIR%im-parent\mp-server\target\mp-server-socket-2.0.war" "%TARGET_DIR%\"

echo [3/5] 复制配置文件模板...
if exist "%PROJECT_DIR%im-parent\mianshi-im-api\src\main\resources\application.properties" (
    xcopy /Y "%PROJECT_DIR%im-parent\mianshi-im-api\src\main\resources\application*.properties" "%TARGET_DIR%\config\"
)

echo [4/5] 生成启动脚本...

REM 生成 imapi 启动脚本
echo @echo off > "%TARGET_DIR%\start-imapi.bat"
echo title 云聊IM - 主API服务 >> "%TARGET_DIR%\start-imapi.bat"
echo java -Xms512m -Xmx2048m -jar imapi.war --server.port=8080 >> "%TARGET_DIR%\start-imapi.bat"
echo pause >> "%TARGET_DIR%\start-imapi.bat"

REM 生成 third-push 启动脚本
echo @echo off > "%TARGET_DIR%\start-third-push.bat"
echo title 云聊IM - 第三方推送服务 >> "%TARGET_DIR%\start-third-push.bat"
echo java -Xms256m -Xmx1024m -jar third-push-socket-2.0.war --server.port=8081 >> "%TARGET_DIR%\start-third-push.bat"
echo pause >> "%TARGET_DIR%\start-third-push.bat"

REM 生成 message-push 启动脚本
echo @echo off > "%TARGET_DIR%\start-message-push.bat"
echo title 云聊IM - 消息推送服务 >> "%TARGET_DIR%\start-message-push.bat"
echo java -Xms256m -Xmx1024m -jar message-push.war --server.port=8082 >> "%TARGET_DIR%\start-message-push.bat"
echo pause >> "%TARGET_DIR%\start-message-push.bat"

REM 生成 mp-server 启动脚本
echo @echo off > "%TARGET_DIR%\start-mp-server.bat"
echo title 云聊IM - MP服务器 >> "%TARGET_DIR%\start-mp-server.bat"
echo java -Xms256m -Xmx1024m -jar mp-server-socket-2.0.war --server.port=8083 >> "%TARGET_DIR%\start-mp-server.bat"
echo pause >> "%TARGET_DIR%\start-mp-server.bat"

REM 生成一键启动所有服务脚本
echo @echo off > "%TARGET_DIR%\start-all.bat"
echo title 云聊IM - 启动所有服务 >> "%TARGET_DIR%\start-all.bat"
echo echo 正在启动云聊IM所有服务... >> "%TARGET_DIR%\start-all.bat"
echo echo. >> "%TARGET_DIR%\start-all.bat"
echo start "云聊IM-主API" cmd /c start-imapi.bat >> "%TARGET_DIR%\start-all.bat"
echo timeout /t 5 >> "%TARGET_DIR%\start-all.bat"
echo start "云聊IM-推送" cmd /c start-third-push.bat >> "%TARGET_DIR%\start-all.bat"
echo timeout /t 3 >> "%TARGET_DIR%\start-all.bat"
echo start "云聊IM-消息" cmd /c start-message-push.bat >> "%TARGET_DIR%\start-all.bat"
echo timeout /t 3 >> "%TARGET_DIR%\start-all.bat"
echo start "云聊IM-MP" cmd /c start-mp-server.bat >> "%TARGET_DIR%\start-all.bat"
echo echo. >> "%TARGET_DIR%\start-all.bat"
echo echo 所有服务已启动! >> "%TARGET_DIR%\start-all.bat"
echo pause >> "%TARGET_DIR%\start-all.bat"

echo [5/5] 生成停止脚本...
echo @echo off > "%TARGET_DIR%\stop-all.bat"
echo title 云聊IM - 停止所有服务 >> "%TARGET_DIR%\stop-all.bat"
echo echo 正在停止云聊IM所有服务... >> "%TARGET_DIR%\stop-all.bat"
echo taskkill /F /FI "WINDOWTITLE eq 云聊IM*" >> "%TARGET_DIR%\stop-all.bat"
echo echo 所有服务已停止! >> "%TARGET_DIR%\stop-all.bat"
echo pause >> "%TARGET_DIR%\stop-all.bat"

echo.
echo ========================================
echo   部署完成!
echo ========================================
echo.
echo 部署目录: %TARGET_DIR%
echo.
echo 快速启动:
echo   1. 配置数据库: 编辑 deploy\config\application.properties
echo   2. 启动所有服务: 运行 deploy\start-all.bat
echo   3. 停止所有服务: 运行 deploy\stop-all.bat
echo.
echo 单独启动:
echo   - 主API服务: deploy\start-imapi.bat
echo   - 推送服务: deploy\start-third-push.bat
echo   - 消息服务: deploy\start-message-push.bat
echo   - MP服务: deploy\start-mp-server.bat
echo.
echo 访问地址:
echo   - 主API: http://localhost:8080
echo   - Swagger: http://localhost:8080/swagger-ui.html
echo.
echo ========================================
echo.
pause
