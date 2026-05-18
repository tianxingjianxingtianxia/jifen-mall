@echo off
title MiniAppTest
echo Launching DevTools...
"D:\Program Files (x86)\Tencent\微信web开发者工具\cli.bat" auto --project "J:\2026\jifen\jifen-miniapp" --auto-port 9420
echo Waiting 5s...
timeout /t 5 /nobreak >nul
echo Running tests...
cd /d "%~dp0"
node miniapp\auto-test.cjs
echo Done.
pause
