import mysql.connector

def check_icd_codes_schema():
    try:
        db = mysql.connector.connect(
            host='hcc-dev-rds.ch2uy8ukk87u.us-east-1.rds.amazonaws.com',
            user='admin',
            password='=2!]+7mrLTh+[JRnEKgxS)X@iY5t*ov1',
            database='hccdb'
        )
        cursor = db.cursor()
        cursor.execute("DESCRIBE icd_codes")
        columns = cursor.fetchall()
        print("Schema for icd_codes:")
        for col in columns:
            print(col)
        db.close()
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    check_icd_codes_schema()
