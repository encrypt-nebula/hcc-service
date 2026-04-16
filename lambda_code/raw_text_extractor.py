import json
import boto3
import os
import re
import urllib.request

# Clients
bedrock_runtime = boto3.client('bedrock-runtime', region_name='us-east-1')
s3 = boto3.client('s3')
secrets_client = boto3.client('secretsmanager')

# Env Vars
PROJECT_NAME_ENV = os.environ.get('PROJECT_NAME', 'hcc-platform')
INTERNAL_API_KEY_ARN = os.environ.get('INTERNAL_API_KEY_ARN')

# External API Configuration
EXTERNAL_API_URL = os.environ.get('EXTERNAL_API_URL', 'http://13.235.138.74:9001/extract-data')

import datetime

def format_date(date_str):
    """Attempt to normalize date strings to YYYY-MM-DD for Java LocalDate."""
    if not date_str or str(date_str).lower() in ["unknown", "none", "null", ""]:
        return "Unknown"
    
    date_str = str(date_str).strip()
    
    # Clean up common natural language artifacts (e.g., "11th", "Tuesday,")
    # Remove day names
    date_str = re.sub(r'^(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)[,\s]*', '', date_str, flags=re.IGNORECASE)
    # Remove ordinal suffixes (1st, 2nd, 3rd, 4th...)
    date_str = re.sub(r'(\d+)(st|nd|rd|th)', r'\1', date_str)
    
    # 1. Try ISO format (YYYY-MM-DD) directly
    try:
        return datetime.date.fromisoformat(date_str[:10]).isoformat()
    except:
        pass

    # 2. Try common numeric formats with regex (MM/DD/YYYY, DD-MM-YYYY)
    try:
        match = re.search(r'(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})', date_str)
        if match:
            p1, p2, year_str = match.groups()
            year = int(year_str)
            if len(year_str) == 2:
                year = 2000 + year
            
            # Try MM/DD/YYYY then DD/MM/YYYY
            for d, m in [(int(p2), int(p1)), (int(p1), int(p2))]:
                try:
                    return datetime.date(year, m, d).isoformat()
                except ValueError:
                    continue
    except:
        pass

    # 3. Try natural language parsing (e.g., "November 11 2025")
    formats = ["%B %d %Y", "%b %d %Y", "%d %B %Y", "%d %b %Y", "%m %d %Y"]
    for fmt in formats:
        try:
            return datetime.datetime.strptime(date_str.replace(',', ''), fmt).date().isoformat()
        except:
            continue

    return "Unknown"

def get_internal_api_key():
    """Fetches the shared internal API key from Secrets Manager."""
    if not INTERNAL_API_KEY_ARN:
        return "hcc-internal-secure-key-2026"
    try:
        response = secrets_client.get_secret_value(SecretId=INTERNAL_API_KEY_ARN)
        return response['SecretString']
    except Exception as e:
        print(f"Error fetching API Key secret: {str(e)}")
        return "hcc-internal-secure-key-2026"

def estimate_pdf_pages(file_size_bytes):
    return max(1, file_size_bytes // 50000)

def merge_details(details_list):
    """Merges multiple encounter objects by DOS, deduplicating ICD codes and merging MEAT checks."""
    merged = {}
    for entry in details_list:
        raw_dos = entry.get('dos')
        if not raw_dos: continue
        dos = format_date(raw_dos)
        if dos == "Unknown": continue # Skip if bad date
        if dos not in merged:
            merged[dos] = {
                "dos": dos,
                "extractedIcdCodes": set(),
                "aiSuggestedIcdCode": set(),
                "monitor": False,
                "evaluate": False,
                "assessOrAddress": False,
                "treat": False
            }
        
        # Merge ICD Codes
        ext_codes = entry.get('extractedIcdCodes') or entry.get('extracted_icd_codes') or []
        ai_codes = entry.get('aiSuggestedIcdCode') or entry.get('ai_suggested_icd_code') or []
        merged[dos]["extractedIcdCodes"].update(ext_codes)
        merged[dos]["aiSuggestedIcdCode"].update(ai_codes)

        # Merge MEAT (if any chunk finds it true, it's true)
        merged[dos]["monitor"] = merged[dos]["monitor"] or entry.get('monitor', False)
        merged[dos]["evaluate"] = merged[dos]["evaluate"] or entry.get('evaluate', False)
        merged[dos]["assessOrAddress"] = merged[dos]["assessOrAddress"] or entry.get('assess_address', False) or entry.get('assessOrAddress', False)
        merged[dos]["treat"] = merged[dos]["treat"] or entry.get('treat', False)
    
    return [
        {
            "dos": d,
            "extractedIcdCodes": sorted(list(v["extractedIcdCodes"])),
            "aiSuggestedIcdCode": sorted(list(v["aiSuggestedIcdCode"])),
            "monitor": v["monitor"],
            "evaluate": v["evaluate"],
            "assessOrAddress": v["assessOrAddress"],
            "treat": v["treat"]
        } for d, v in merged.items()
    ]

def send_to_external_api(payload, api_key):
    req = urllib.request.Request(
        EXTERNAL_API_URL,
        data=json.dumps(payload).encode('utf-8'),
        headers={
            'X-Internal-Service-Key': api_key,
            'Content-Type': 'application/json'
        },
        method='POST'
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as res:
            return res.read().decode('utf-8')
    except Exception as e:
        print(f"External API Error: {str(e)}")
        return f"Error: {str(e)}"

def lambda_handler(event, context):
    try:
        if 'body' in event and event['body']:
            body = json.loads(event['body']) if isinstance(event['body'], str) else event['body']
        else:
            return {'statusCode': 400, 'body': json.dumps({'error': 'Missing body'})}

        s3_path = body.get('s3_path')
        path_match = re.search(r's3://([^/]+)/uploads/([^/]+)/([^/]+)/(.+)', s3_path)
        bucket, project_id_str, project_type, file_name = path_match.groups()

        obj_metadata = s3.head_object(Bucket=bucket, Key=f"uploads/{project_id_str}/{project_type}/{file_name}")
        file_content = s3.get_object(Bucket=bucket, Key=f"uploads/{project_id_str}/{project_type}/{file_name}")['Body'].read()
        file_ext = file_name.split('.')[-1].lower()
        
        total_pages = estimate_pdf_pages(obj_metadata['ContentLength']) if file_ext == 'pdf' else 1

        extraction = call_bedrock_nova(file_content, file_ext, file_name)
        
        # Mapping to the payload format the Spring Boot API expects
        merged_details = merge_details(extraction.get('details', []))
        primary_dos = merged_details[0]['dos'] if merged_details else extraction.get('dos', 'Unknown')
        
        final_payload = {
            "fileName": file_name,
            "s3Path": s3_path,
            "totalPages": total_pages,
            "signature": "yes" if extraction.get('signature_found') else "no",
            "credentials": extraction.get('credentials', 'None') or 'None',
            "projectName": PROJECT_NAME_ENV,
            "projectType": project_type,
            "dos": format_date(primary_dos),
            "dob": format_date(extraction.get('dob', 'Unknown')),
            "firstName": extraction.get('first_name', 'Unknown'),
            "lastName": extraction.get('last_name', 'Unknown'),
            "hcinNumber": extraction.get('hcin_number', 'Unknown'),
            "memberId": extraction.get('member_id', 'Unknown'),
            "physicianName": extraction.get('physician_name', 'Unknown'),
            "signedAt": format_date(extraction.get('signed_at')),
            "projectId": int(project_id_str) if project_id_str.isdigit() else project_id_str,
            "workUnitType": "PAGE_RANGE" if project_type == "RETROSPECTIVE" else "PATIENT",
            "details": merged_details,
            "dbStatus": "Skipped (Sunk to API)"
        }

        internal_key = get_internal_api_key()
        api_response = send_to_external_api(final_payload, internal_key)

        return {
            'statusCode': 200,
            'headers': {'Access-Control-Allow-Origin': '*'},
            'body': json.dumps({
                "message": "Data processed",
                "extractedData": final_payload,
                "externalApiResponse": api_response
            })
        }

    except Exception as e:
        print(f"Lambda Error: {str(e)}")
        return {'statusCode': 500, 'body': json.dumps({'error': str(e)})}

def call_bedrock_nova(file_content, file_ext, file_name):
    prompt = """Analyze this clinical document and extract JSON:
1. signature_found: (boolean)
2. credentials: (string or null) e.g. "PT", "MD"
3. first_name, last_name, dob: (strings, DOB format: YYYY-MM-DD)
4. hcin_number, member_id, physician_name, signed_at: (strings, signed_at format: YYYY-MM-DD)
5. details: (list of objects) For each encounter/Date of Service (DOS) found, extract:
   - dos: (string) Date of service in YYYY-MM-DD format.
   - extracted_icd_codes: (list of strings) ICD-10 codes explicitly written.
   - ai_suggested_icd_code: (list of strings) ICD-10 codes suggested by clinical logic.
   - monitor: (boolean) Signs, symptoms, disease progression, or regression documented.
   - evaluate: (boolean) Reviewing test results, medication effectiveness, or response to treatment.
   - assess_address: (boolean) Reviewing records, ordering tests, or providing counseling.
   - treat: (boolean) Prescribing medication, ordering therapies, or referring to specialists.

Return ONLY JSON object."""

    content_blocks = []
    
    # Helper to detect image format from magic bytes
    def detect_format(content, ext):
        if content.startswith(b'\xff\xd8'): return 'jpeg'
        if content.startswith(b'\x89PNG\r\n\x1a\n'): return 'png'
        if content.startswith(b'RIFF') and content[8:12] == b'WEBP': return 'webp'
        if content.startswith(b'GIF87a') or content.startswith(b'GIF89a'): return 'gif'
        if b'ftypavif' in content[4:12]: return 'avif'
        
        # Fallback to extension mapping
        ext_map = {'jpg': 'jpeg', 'jpeg': 'jpeg', 'png': 'png', 'webp': 'webp', 'gif': 'gif', 'avif': 'avif'}
        return ext_map.get(ext.lower())

    detected_format = detect_format(file_content, file_ext)

    if file_ext in ['pdf', 'csv', 'txt', 'docx', 'xlsx', 'md']:
        content_blocks.append({'document': {'name': 'Doc', 'format': file_ext, 'source': {'bytes': file_content}}})
    elif detected_format in ['png', 'jpeg', 'webp', 'gif']:
        content_blocks.append({'image': {'format': detected_format, 'source': {'bytes': file_content}}})
    elif detected_format == 'avif':
        # Bedrock Converse does NOT support AVIF. Sending it with a false format tag causes a ValidationException.
        # We skip the image block and inform the model via text to avoid a hard crash.
        print(f"Error: AVIF format detected for {file_ext}. This format is not supported by Bedrock Converse API.")
        content_blocks.append({'text': f"[Warning: The uploaded file {file_name} is in AVIF format, which is currently not supported for direct processing. Please convert to JPG or PNG.]"})
    else:
        content_blocks.append({'text': f"File context (unsupported or unrecognized format): {file_ext}"})

    content_blocks.append({'text': prompt})

    try:
        response = bedrock_runtime.converse(
            modelId="amazon.nova-lite-v1:0",
            messages=[{'role': 'user', 'content': content_blocks}],
            inferenceConfig={'temperature': 0}
        )
        text = response['output']['message']['content'][0]['text']
        return json.loads(re.sub(r'```json\s*|\s*```', '', text).strip())
    except Exception as e:
        print(f"Bedrock Error: {str(e)}")
        return {"signature_found": False, "details": []}
