import mysql.connector
import sys

def check():
    try:
        db = mysql.connector.connect(
            host='hcc-dev-rds.ch2uy8ukk87u.us-east-1.rds.amazonaws.com',
            user='admin',
            password='=2!]+7mrLTh+[JRnEKgxS)X@iY5t*ov1',
            database='hccdb'
        )
        c = db.cursor()
        c.execute("SHOW TABLES")
        tables = c.fetchall()
        print(f"Tables: {tables}")
        for (table,) in tables:
            c.execute(f"DESCRIBE {table}")
            print(f"Table: {table}")
            for col in c.fetchall():
                print(col)
        db.close()
    except Exception as e:
        print(f"Error: {e}")

if __name__ == '__main__':
    check()
