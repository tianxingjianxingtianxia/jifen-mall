@echo off
chcp 65001 >nul
title 积分商城 - 小程序自动化验收测试

echo ══════════════════════════════════════════
echo   积分商城 微信小程序 自动化验收测试
echo ══════════════════════════════════════════
echo.

echo [1/4] 安装依赖...
cd /d "%~dp0"
call npm install 2>&1
if %errorlevel% neq 0 (
    echo ❌ npm install 失败
    pause
    exit /b 1
)
echo ✅ 依赖安装完成
echo.

echo [2/4] 启动微信开发者工具自动化窗口...
set CLI_PATH=D:\Program Files (x86)\Tencent\微信web开发者工具\cli.bat
set PROJECT_PATH=J:\2026\jifen\jifen-miniapp

"%CLI_PATH%" auto --project "%PROJECT_PATH%" --auto-port 9420
echo ✅ 开发者工具已启动
echo.

echo [3/4] 等待开发者工具就绪（5秒）...
timeout /t 5 /nobreak >nul
echo.

echo [4/4] 开始自动化测试...
echo   测试过程中请不要关闭开发者工具窗口
echo.
node miniapp_test.cjs
set EXIT_CODE=%errorlevel%

if %EXIT_CODE% equ 0 (
    echo ✅ 全部测试通过！
) else (
    echo ⚠️ 有 %EXIT_CODE% 个测试失败，详情见上方
)

echo.
echo 📸 截图位置：screenshots\
echo.
pause
