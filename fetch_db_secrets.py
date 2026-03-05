import boto3
import json

def get_secret(secret_name, region_name="us-east-1"):
    client = boto3.client('secretsmanager', region_name=region_name)
    try:
        response = client.get_secret_value(SecretId=secret_name)
        if 'SecretString' in response:
            return json.loads(response['SecretString'])
    except Exception as e:
        print(f"Error fetching secret '{secret_name}': {e}")
    return None

if __name__ == "__main__":
    secret = get_secret("hcc/dev/db")
    if secret:
        print(json.dumps(secret, indent=4))
    else:
        # try the other secret name from bootstrap_db.py
        secret2 = get_secret("hcc-platform-dev-db-password")
        if secret2:
            print(json.dumps(secret2, indent=4))
