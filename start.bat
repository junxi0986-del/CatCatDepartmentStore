@echo off
chcp 65001 >nul
title 猫猫百货商城 - 一键启动
cd /d "%~dp0"

rem 优先使用项目自带的 Python 解释器（不依赖系统 PATH）
set "PY=%~dp0ecommerce_rag_customer\.venv\Scripts\python.exe"
if not exist "%PY%" set "PY=python"

"%PY%" start.py

echo.
echo 脚本已退出，按任意键关闭窗口...
pause >nul
