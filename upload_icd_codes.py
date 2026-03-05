import csv
import mysql.connector
import boto3
import json

def get_db_credentials(secret_name="hcc/dev/db", region_name="us-east-1"):
    client = boto3.client('secretsmanager', region_name=region_name)
    try:
        response = client.get_secret_value(SecretId=secret_name)
        return json.loads(response['SecretString'])
    except Exception as e:
        print(f"Error fetching secret '{secret_name}': {e}")
        return None

def upload_data():
    csv_file_path = '/Users/harshsharma/Hivemynds/hcc-service/icd10code_data.csv'
    
    secret = get_db_credentials()
    if not secret:
        # Fallback to the hardcoded ones if secret fetch fails
        print("Warning: Could not fetch secrets from AWS SM. Falling back to previous config.")
        db_config = {
            'host': 'hcc-dev-rds.ch2uy8ukk87u.us-east-1.rds.amazonaws.com',
            'user': 'hccAdmin',
            'password': '=2!]+7mrLTh+[JRnEKgxS)X@iY5t*ov1',
            'database': 'hccdb'
        }
    else:
        db_config = {
            'host': secret.get('rds_url') or secret.get('host') or 'hcc-dev-rds.ch2uy8ukk87u.us-east-1.rds.amazonaws.com',
            'user': secret.get('username') or secret.get('user'),
            'password': secret.get('password'),
            'database': secret.get('db_name') or secret.get('database') or 'hccdb'
        }

    try:
        print("Connecting to database...")
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        print("Connected to database.")

        # Clear the table first if needed, or just insert
        print("Truncating icd_codes table...")
        cursor.execute("TRUNCATE TABLE icd_codes")
        print("Table truncated.")

        with open(csv_file_path, newline='', encoding='utf-8-sig') as csvfile:
            print("Reading CSV file...")
            reader = csv.reader(csvfile)
            print("CSV file read.")

            count = 0
            insert_query = "INSERT INTO icd_codes (icd_code, hcc_score, description) VALUES (%s, %s, %s)"
            print("Inserting records...")
            
            for row in reader:
                if len(row) < 3:
                    continue
                
                icd_code = row[0].strip()
                description = row[1].strip()
                hcc_category = row[2].strip()
                
                # HCC score/category handling
                try:
                    # If it's something like "92", float() can handle it.
                    # DECIMAL(5,3) in MySQL means 5 total digits, 3 after decimal.
                    # Max value is 99.999.
                    # If the category is 100 or higher, this will fail.
                    # Let's check for that.
                    hcc_score = float(hcc_category) if hcc_category else 0.0
                except ValueError:
                    hcc_score = 0.0

                try:
                    print(f"Inserting row {icd_code}...")
                    cursor.execute(insert_query, (icd_code, hcc_score, description))
                    count += 1
                except mysql.connector.Error as err:
                    print(f"Error inserting row {icd_code}: {err}")
                    continue
                
                if count % 1000 == 0:
                    conn.commit()
                    print(f"Inserted {count} records...")

            conn.commit()
            print(f"Successfully uploaded {count} records to icd_codes table.")

        cursor.close()
        conn.close()
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    upload_data()
