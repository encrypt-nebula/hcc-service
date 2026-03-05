import mysql.connector

def verify():
    db_config = {
        'host': 'hcc-dev-rds.ch2uy8ukk87u.us-east-1.rds.amazonaws.com',
        'user': 'admin',
        'password': '=2!]+7mrLTh+[JRnEKgxS)X@iY5t*ov1',
        'database': 'hccdb'
    }

    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM icd_codes")
        count = cursor.fetchone()[0]
        print(f"Total rows in icd_codes: {count}")
        
        if count > 0:
            cursor.execute("SELECT * FROM icd_codes LIMIT 5")
            rows = cursor.fetchall()
            for r in rows:
                print(r)

        cursor.close()
        conn.close()
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    verify()
