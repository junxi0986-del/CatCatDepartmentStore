#!/usr/bin/env python3
import subprocess
import time
import os
import sys
import socket
import threading
from datetime import datetime

# 项目配置
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
SERVER_DIR = os.path.join(BASE_DIR, 'ecommerce-server')
RAG_DIR = os.path.join(BASE_DIR, 'ecommerce_rag_customer')
FRONTEND_DIR = os.path.join(BASE_DIR, 'ecommerce-frontend')
ADMIN_DIR = os.path.join(BASE_DIR, 'ecommerce-admin')

# 端口配置
MYSQL_PORT = 3306
SERVER_PORT = 8083
RAG_PORT = 5050
FRONTEND_PORT = 5173
ADMIN_PORT = 5174

# 进程对象
processes = []
stop_event = threading.Event()

def log(msg, level='INFO'):
    """日志输出"""
    print(f"[{datetime.now().strftime('%H:%M:%S')}] [{level}] {msg}")

def check_port(port):
    """检查端口是否被占用"""
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(1)
        result = sock.connect_ex(('localhost', port))
        sock.close()
        return result == 0  # True表示端口被占用
    except Exception:
        return False

def wait_port(port, timeout=60):
    """等待端口就绪"""
    start = time.time()
    while time.time() - start < timeout:
        if check_port(port):
            return True
        time.sleep(1)
    return False

def run_command(cmd, cwd=None, env=None, name='process'):
    """运行命令"""
    log(f"启动 {name}: {cmd}")
    try:
        process = subprocess.Popen(
            cmd,
            cwd=cwd,
            env=env,
            shell=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True
        )
        
        # 输出线程
        def output_reader():
            try:
                while process.poll() is None and not stop_event.is_set():
                    line = process.stdout.readline()
                    if line:
                        print(f"[{name}] {line.strip()}")
            except:
                pass
        
        threading.Thread(target=output_reader, daemon=True).start()
        processes.append((name, process))
        return process
    except Exception as e:
        log(f"启动 {name} 失败: {e}", 'ERROR')
        return None

def stop_all():
    """停止所有进程"""
    stop_event.set()
    log("正在停止所有服务...")
    for name, process in processes:
        try:
            process.terminate()
            process.wait(timeout=5)
            log(f"{name} 已停止")
        except:
            try:
                process.kill()
                log(f"{name} 已强制终止")
            except:
                pass

def main():
    print("=" * 60)
    print("        猫猫百货商城 - 一键启动脚本")
    print("=" * 60)
    
    # 注册退出信号
    import signal
    signal.signal(signal.SIGINT, lambda sig, frame: stop_all())
    signal.signal(signal.SIGTERM, lambda sig, frame: stop_all())
    
    # 1. 检查MySQL
    log("检查MySQL服务...")
    if not check_port(MYSQL_PORT):
        log(f"警告: MySQL端口 {MYSQL_PORT} 未开放，请确保MySQL已启动", 'WARNING')
        # input("按回车继续...")
    
    # 2. 检查端口占用
    occupied = []
    if check_port(SERVER_PORT):
        occupied.append(f"后端 {SERVER_PORT}")
    if check_port(RAG_PORT):
        occupied.append(f"RAG {RAG_PORT}")
    if check_port(FRONTEND_PORT):
        occupied.append(f"用户端 {FRONTEND_PORT}")
    if check_port(ADMIN_PORT):
        occupied.append(f"管理端 {ADMIN_PORT}")
    
    if occupied:
        log(f"以下端口已被占用: {', '.join(occupied)}", 'WARNING')
        log("建议关闭占用端口的程序后再启动", 'WARNING')
        # input("按回车继续...")
    
    try:
        # 3. 启动Java后端
        log(f"启动Java后端 (端口 {SERVER_PORT})...")
        # 使用Maven运行，需要配置好环境
        server_env = os.environ.copy()
        server_env['MAVEN_OPTS'] = '-Xms512m -Xmx1024m'
        run_command(
            'mvn spring-boot:run',
            cwd=SERVER_DIR,
            env=server_env,
            name='Java后端'
        )
        
        # 等待后端启动
        log(f"等待后端端口 {SERVER_PORT} 就绪...")
        if not wait_port(SERVER_PORT, timeout=120):
            log("后端启动超时", 'ERROR')
            stop_all()
            return
        
        # 4. 启动RAG服务
        log(f"启动RAG服务 (端口 {RAG_PORT})...")
        rag_env = os.environ.copy()
        rag_env['HF_HUB_OFFLINE'] = '1'
        rag_env['DEEPSEEK_API_KEY'] = os.environ.get('DEEPSEEK_API_KEY', '')
        run_command(
            f'python {os.path.join(RAG_DIR, "main.py")} api --port {RAG_PORT}',
            cwd=RAG_DIR,
            env=rag_env,
            name='RAG服务'
        )
        
        # 等待RAG启动
        log(f"等待RAG端口 {RAG_PORT} 就绪...")
        if not wait_port(RAG_PORT, timeout=60):
            log("RAG服务启动超时", 'WARNING')
        
        # 5. 启动用户端前端
        log(f"启动用户端前端 (端口 {FRONTEND_PORT})...")
        run_command(
            'npm run dev',
            cwd=FRONTEND_DIR,
            name='用户端前端'
        )
        
        # 6. 启动管理端前端
        log(f"启动管理端前端 (端口 {ADMIN_PORT})...")
        run_command(
            'npm run dev',
            cwd=ADMIN_DIR,
            name='管理端前端'
        )
        
        print("=" * 60)
        log("所有服务启动完成！")
        log(f"用户端: http://localhost:{FRONTEND_PORT}")
        log(f"管理端: http://localhost:{ADMIN_PORT}")
        log(f"后端API: http://localhost:{SERVER_PORT}")
        log(f"RAG服务: http://localhost:{RAG_PORT}")
        print("=" * 60)
        log("按 Ctrl+C 停止所有服务")
        
        # 等待
        while not stop_event.is_set():
            time.sleep(1)
            
    except Exception as e:
        log(f"启动过程发生错误: {e}", 'ERROR')
        stop_all()

if __name__ == '__main__':
    main()
