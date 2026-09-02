
from auth import init_db, register_user

def create_sample_users():
    init_db()
    
    users = [
        ("admin", "admin123", "admin@example.com"),
        ("user1", "password1", "user1@example.com"),
        ("test", "test123", "test@example.com"),
    ]
    
    for username, password, email in users:
        success, msg = register_user(username, password, email)
        status = "✅" if success else "⚠️"
        print(f"{status} 用户 {username}: {msg}")
    
    print("\n用户数据库初始化完成！")
    print("示例用户账号：")
    print("  - 用户名: admin, 密码: admin123")
    print("  - 用户名: user1, 密码: password1")
    print("  - 用户名: test,  密码: test123")

if __name__ == "__main__":
    create_sample_users()
