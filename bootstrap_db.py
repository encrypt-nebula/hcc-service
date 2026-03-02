import mysql.connector
import boto3
import json

def get_db_secret():
    client = boto3.client('secretsmanager', region_name='us-east-1')
    response = client.get_secret_value(SecretId='hcc-platform-dev-db-password')
    return json.loads(response['SecretString'])

def bootstrap():
    secret = get_db_secret()
    # Also need endpoint
    endpoint = "hcc-dev-rds.ch2uy8ukk87u.us-east-1.rds.amazonaws.com"
    
    db = mysql.connector.connect(
        host=endpoint,
        user="admin",
        password=secret['password'],
        database="hcc_platform"
    )
    cursor = db.cursor()
    
    # 1. Ensure Company exists
    cursor.execute("SELECT id FROM company WHERE id=1")
    if not cursor.fetchone():
        cursor.execute("INSERT INTO company (id, name, address) VALUES (1, 'Default Company', '123 Test St')")
        print("Inserted Company 1")
    
    # 2. Ensure Project 1 exists
    cursor.execute("SELECT id FROM projects WHERE id=1")
    if not cursor.fetchone():
        cursor.execute("INSERT INTO projects (id, project_name, project_type, company_id) VALUES (1, 'Default Project', 'PROSPECTIVE', 1)")
        print("Inserted Project 1")
    
    db.commit()
    cursor.close()
    db.close()

if __name__ == "__main__":
    bootstrap()
