@echo off
chcp 65001 >nul
title 积分商城 - 小程序自动化测试启动器

echo ============================================
echo   积分商城 微信小程序自动化测试
echo ============================================
echo.

echo [1/3] 启动微信开发者工具自动化窗口...
set CLI_PATH=D:\Program Files (x86)\Tencent\微信web开发者工具\cli.bat
set PROJECT_PATH=J:\2026\jifen\jifen-miniapp

"%CLI_PATH%" auto --project "%PROJECT_PATH%" --auto-port 9420
echo ✅ 开发者工具已启动（请稍候...）
timeout /t 3 /nobreak >nul
echo.

echo [2/3] 连接开发者工具并执行测试...
cd /d "%~dp0"
node miniapp/auto-test.cjs
set EXIT_CODE=%errorlevel%
echo.

echo [3/3] 测试完成
if %EXIT_CODE% equ 0 (
    echo ✅ 全部测试通过！
) else (
    echo ⚠️ 有 %EXIT_CODE% 个测试失败
)

echo.
pause
