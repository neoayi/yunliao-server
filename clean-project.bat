@echo off
REM ===================================================================
REM 云聊 IM 服务器 - 项目清理脚本
REM 功能: 删除编译生成的文件,保留源代码和必要配置
REM ===================================================================
setlocal enabledelayedexpansion

echo.
echo ========================================
echo   云聊 IM 项目清理工具
echo ========================================
echo.
echo 此脚本将删除以下内容:
echo   - 所有 target 目录 (Maven编译输出)
echo   - logs 目录 (运行日志)
echo   - deploy 目录 (部署文件)
echo   - .class 文件 (编译后的字节码)
echo   - IDE 临时文件 (.idea, *.iml)
echo.
echo 保留内容:
echo   - 所有源代码 (src/)
echo   - Maven 配置 (pom.xml)
echo   - 配置文件 (*.properties, *.xml, *.yml)
echo   - 文档文件 (*.md, README)
echo   - lib 目录 (第三方依赖)
echo.

set /p confirm="确认要清理项目吗? (Y/N): "
if /i not "%confirm%"=="Y" (
    echo 已取消清理操作
    goto :end
)

echo.
echo [1/6] 清理 Maven target 目录...
set count=0
for /d /r %%d in (target) do (
    if exist "%%d" (
        echo 删除: %%d
        rd /s /q "%%d" 2>nul
        set /a count+=1
    )
)
echo 已删除 %count% 个 target 目录

echo.
echo [2/6] 清理日志目录...
if exist "logs" (
    echo 删除: logs\
    rd /s /q "logs" 2>nul
)
if exist "log" (
    echo 删除: log\
    rd /s /q "log" 2>nul
)

echo.
echo [3/6] 清理部署目录...
if exist "deploy" (
    echo 删除: deploy\
    rd /s /q "deploy" 2>nul
)

echo.
echo [4/6] 清理上传文件目录...
if exist "upload\*.*" (
    echo 清空: upload\ (保留目录结构)
    del /q /s "upload\*.*" 2>nul
)

echo.
echo [5/6] 清理 IDE 文件...
set count=0
for /r %%f in (*.iml) do (
    if exist "%%f" (
        echo 删除: %%f
        del /q "%%f" 2>nul
        set /a count+=1
    )
)
if exist ".idea" (
    echo 删除: .idea\
    rd /s /q ".idea" 2>nul
)
if exist ".vscode" (
    echo 删除: .vscode\
    rd /s /q ".vscode" 2>nul
)
if exist ".settings" (
    echo 删除: .settings\
    rd /s /q ".settings" 2>nul
)
if exist ".classpath" (
    del /q ".classpath" 2>nul
)
if exist ".project" (
    del /q ".project" 2>nul
)
echo 已删除 %count% 个 .iml 文件

echo.
echo [6/6] 清理编译文件...
set count=0
for /r %%f in (*.class) do (
    if exist "%%f" (
        del /q "%%f" 2>nul
        set /a count+=1
    )
)
echo 已删除 %count% 个 .class 文件

echo.
echo ========================================
echo   清理完成!
echo ========================================
echo.

REM 统计保留的文件
echo 项目统计:
echo.

set java_count=0
for /r %%f in (*.java) do set /a java_count+=1
echo   Java 源文件: %java_count% 个

set pom_count=0
for /r %%f in (pom.xml) do set /a pom_count+=1
echo   Maven 配置: %pom_count% 个

set xml_count=0
for /r %%f in (*.xml) do set /a xml_count+=1
echo   XML 配置文件: %xml_count% 个

set prop_count=0
for /r %%f in (*.properties) do set /a prop_count+=1
echo   Properties 文件: %prop_count% 个

set md_count=0
for /r %%f in (*.md) do set /a md_count+=1
echo   Markdown 文档: %md_count% 个

echo.
echo 建议下一步操作:
echo   1. 提交到 Git 仓库: git add . ^&^& git commit -m "Clean build"
echo   2. 重新编译项目: mvn clean install -DskipTests
echo   3. 创建压缩包: 使用 7-Zip 或 WinRAR 压缩项目文件夹
echo.

:end
pause
