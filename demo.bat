@echo off
chcp 65001 >nul
title 猫猫百货商城 - 演示模式
cd /d "%~dp0"

rem 与 start.bat 相同的解释器解析：优先项目自带 Python，不依赖系统 PATH
set "PY=%~dp0ecommerce_rag_customer\.venv\Scripts\python.exe"
if not exist "%PY%" set "PY=python"

echo ============================================
echo   猫猫百货商城 - 答辩演示一键启动
echo ============================================
echo.
echo [1/3] 正在启动全部服务...
echo       （弹出的服务窗口请保持开启，Ctrl+C 可停止全部服务）
start "catcat-services" cmd /k ""%PY%" start.py"

echo [2/3] 等待服务就绪（首次启动约 1-2 分钟，请勿关闭本窗口）...
set /a tries=0

:waitloop
timeout /t 5 /nobreak >nul
powershell -NoProfile -Command "$c=New-Object Net.Sockets.TcpClient; $r1=$c.ConnectAsync('127.0.0.1',5173).Wait(800) -and $c.Connected; $c.Close(); $c2=New-Object Net.Sockets.TcpClient; $r2=$c2.ConnectAsync('127.0.0.1',5174).Wait(800) -and $c2.Connected; $c2.Close(); if($r1 -and $r2){exit 0}else{exit 1}"
if errorlevel 1 goto inc
goto ready

:inc
set /a tries+=1
if %tries% geq 36 goto timeout
goto waitloop

:ready
echo [3/3] 服务就绪，正在打开演示页面...
start "" http://localhost:5174/login
timeout /t 2 /nobreak >nul
start "" http://localhost:5173

echo.
echo ============================================
echo   演示环境已就绪！
echo   用户端  http://localhost:5173
echo   管理端  http://localhost:5174  账号 admin / 123456
echo ============================================
echo.
echo 停止演示：在 catcat-services 窗口按 Ctrl+C（或直接关闭该窗口）
echo 本窗口可直接关闭。
pause >nul
exit /b 0

:timeout
echo.
echo 等待超时：服务未在 3 分钟内就绪。
echo 常见原因：MySQL 服务未启动（services.msc 里启动 MySQL 后重试）。
echo 详细报错请查看 catcat-services 窗口中的日志。
pause >nul
exit /b 1
