#!/usr/bin/env python3

"""
db_restore-local2prod.py
Uploads local database to production server

Usage:
    python3 scripts/db_restore-local2prod.py
"""

import requests
import json
import os
import sys
from datetime import datetime
from pathlib import Path

# Configuration
LOCAL_URL = "http://localhost:8585"
PROD_URL = "https://spring.opencodingsociety.com"
PROD_LOGIN_URL = f"{PROD_URL}/login"
PROD_IMPORT_URL = f"{PROD_URL}/api/imports/manual"
LOCAL_EXPORT_URL = f"{LOCAL_URL}/api/exports/getAll"

# Credentials
ADMIN_UID = "toby"

def load_admin_password():
    """Load ADMIN_PASSWORD from .env file"""
    env_file = Path(__file__).parent.parent / ".env"

    if not env_file.exists():
        print(f" Error: .env file not found at {env_file}")
        sys.exit(1)

    with open(env_file, "r") as f:
        for line in f:
            line = line.strip()
            if line.startswith("ADMIN_PASSWORD="):
                password = line.split("=", 1)[1].strip()
                # Remove quotes if present
                password = password.strip('"').strip("'")
                return password

    print(" Error: ADMIN_PASSWORD not found in .env file")
    sys.exit(1)

def check_local_server():
    """Check if local Spring Boot server is running"""
    try:
        response = requests.get(f"{LOCAL_URL}/api/exports/getAll", timeout=5)
        return response.status_code != 404
    except requests.exceptions.RequestException:
        return False

def export_local_database():
    """Export local database to JSON file"""
    print(" Exporting local database...")

    if not check_local_server():
        print(" Error: Local Spring Boot server is not running on port 8585")
        print("   Please start it first: ./mvnw spring-boot:run")
        sys.exit(1)

    try:
        response = requests.get(LOCAL_EXPORT_URL, timeout=30)
        response.raise_for_status()
        data = response.json()

        # Create backups directory if it doesn't exist
        backups_dir = Path(__file__).parent.parent / "volumes" / "backups"
        backups_dir.mkdir(parents=True, exist_ok=True)

        # Save with timestamp
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        export_file = backups_dir / f"local_export_{timestamp}.json"

        with open(export_file, "w") as f:
            json.dump(data, f, indent=2)

        print(f" Local database exported to: {export_file}")
        print(f"  Tables exported: {len(data)}")
        print(f"  File size: {export_file.stat().st_size / 1024:.2f} KB")

        return export_file, data

    except requests.exceptions.RequestException as e:
        print(f" Error exporting local database: {e}")
        sys.exit(1)
    except Exception as e:
        print(f" Unexpected error: {e}")
        sys.exit(1)

def authenticate_to_production(password):
    """Authenticate to production server using form login and get session cookie"""
    print("\n Authenticating to production server...")

    # Use form-based login (not JWT) because @JsonIgnore on password field
    # prevents JSON authentication from working
    auth_data = {
        "username": ADMIN_UID,
        "password": password
    }

    try:
        # Create a session to handle cookies and redirects
        session = requests.Session()
        response = session.post(PROD_LOGIN_URL, data=auth_data, timeout=10, allow_redirects=False)

        # Form login returns 302 redirect on success
        if response.status_code == 302 and "sess_java_spring" in response.cookies:
            print(f" Authenticated as '{ADMIN_UID}'")
            return session.cookies
        else:
            print(f" Authentication failed: HTTP {response.status_code}")
            print(f"   Response: {response.text[:200]}")
            sys.exit(1)

    except requests.exceptions.HTTPError as e:
        print(f" Authentication failed: HTTP {e.response.status_code}")
        print(f"   Response: {e.response.text}")
        sys.exit(1)
    except requests.exceptions.RequestException as e:
        print(f" Error connecting to production server: {e}")
        sys.exit(1)

def upload_to_production(export_file, cookies):
    """Upload JSON file to production server"""
    print(f"\n Uploading database to production...")
    print(f"   File: {export_file.name}")
    print(f"   Target: {PROD_IMPORT_URL}")

    try:
        with open(export_file, "rb") as f:
            files = {"file": (export_file.name, f, "application/json")}
            response = requests.post(
                PROD_IMPORT_URL,
                files=files,
                cookies=cookies,
                timeout=120  # 2 minute timeout for large uploads
            )
            response.raise_for_status()

            print(" Database uploaded successfully!")
            print(f"   Response status: {response.status_code}")

            # Try to show response content if it's text
            if "text/html" in response.headers.get("Content-Type", ""):
                if "success" in response.text.lower():
                    print("   Import status: SUCCESS")
                elif "error" in response.text.lower():
                    print("   Import status: ERROR (check production logs)")
                    print(f"   Response preview: {response.text[:200]}")
            else:
                print(f"   Response: {response.text[:200]}")

    except requests.exceptions.HTTPError as e:
        print(f" Upload failed: HTTP {e.response.status_code}")
        print(f"   Response: {e.response.text[:500]}")
        sys.exit(1)
    except requests.exceptions.RequestException as e:
        print(f" Error uploading to production: {e}")
        sys.exit(1)

def main():
    print("=" * 60)
    print("DATABASE RESTORE: Local → Production")
    print("=" * 60)
    print()

    # Step 1: Load credentials
    password = load_admin_password()

    # Step 2: Export local database
    export_file, data = export_local_database()

    # Step 3: Authenticate to production
    cookies = authenticate_to_production(password)

    # Step 4: Upload to production
    upload_to_production(export_file, cookies)

    print()
    print("=" * 60)
    print("RESTORE COMPLETE")
    print("=" * 60)
    print()
    print(f"Local database has been uploaded to production:")
    print(f"  Production URL: {PROD_URL}")
    print(f"  Backup saved: {export_file}")
    print()

if __name__ == "__main__":
    main()
