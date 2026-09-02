
import sqlite3

conn = sqlite3.connect('chroma_db/chroma.sqlite3')
cursor = conn.cursor()

print("Chroma数据库表:")
cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
for row in cursor.fetchall():
    print(row)

cursor.execute("SELECT COUNT(*) FROM collections")
count = cursor.fetchone()[0]
print(f"\ncollections表记录数: {count}")

cursor.execute("SELECT COUNT(*) FROM embeddings")
count = cursor.fetchone()[0]
print(f"embeddings表记录数: {count}")

conn.close()
