@echo off
chcp 65001 >nul
title 猫猫百货商城 - 一键启动
cd /d "%~dp0"
python start.py
echo.
echo 脚本已退出，按任意键关闭窗口...
pause >nul
