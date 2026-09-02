@echo off
chcp 65001 >nul
echo ========================================
echo   电商智能客服RAG服务启动脚本
echo ========================================
echo.

cd /d "%~dp0"

echo 请选择启动方式：
echo [1] Flask API服务
echo [2] Streamlit界面
echo [3] 构建知识库
echo [4] 安装依赖
echo.

set /p choice="请输入选项 (1-4): "

if "%choice%"=="1" goto api
if "%choice%"=="2" goto streamlit
if "%choice%"=="3" goto build
if "%choice%"=="4" goto install
goto end

:api
echo.
echo 启动Flask API服务...
echo.
if "%DEEPSEEK_API_KEY%"=="" (
    set /p API_KEY="请输入DeepSeek API Key: "
    python main.py api --api-key %API_KEY% --port 5000
) else (
    python main.py api --port 5000
)
goto end

:streamlit
echo.
echo 启动Streamlit界面...
echo.
streamlit run ui.py
goto end

:build
echo.
echo 构建知识库...
echo.
python main.py build
goto end

:install
echo.
echo 安装依赖...
echo.
pip install -r requirements.txt
echo.
echo 安装完成！
goto end

:end
pause
