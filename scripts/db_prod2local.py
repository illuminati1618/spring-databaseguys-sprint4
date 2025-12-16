#!/usr/bin/env python3
"""
db_migrate.py - Spring Boot Database Migration

Migrates database with remote data import.
- Backs up current database
- Drops all tables
- Creates fresh schema via Spring Boot
- Imports data from remote database

Usage:
    scripts/db_migrate.py                # Import from remote (default)
    FORCE_YES=true scripts/db_migrate.py # Skip confirmation
"""

import os
import sys
import shutil
import subprocess
import signal
import time
import socket
import json
import sqlite3
import requests
from datetime import datetime
from pathlib import Path

# Configuration
PROJECT_ROOT = Path(__file__).parent.parent
DB_FILE = PROJECT_ROOT / "volumes" / "sqlite.db"
BACKUP_DIR = PROJECT_ROOT / "volumes" / "backups"
JSON_DATA_FILE = PROJECT_ROOT / "volumes" / "data.json"
SKIP_FLAG_FILE = PROJECT_ROOT / "volumes" / ".skip-modelinit"
SPRING_PORT = 8585
LOG_FILE = "/tmp/db_migrate.log"

# Remote database configuration
PROD_URL = "https://spring.opencodingsociety.com"
PROD_LOGIN_URL = f"{PROD_URL}/login"  # Changed from /authenticate
DATA_URL = f"{PROD_URL}/api/exports/getAll"

# Credentials
ADMIN_UID = "toby"


def print_header(title):
    """Print a formatted header"""
    print("\n" + "=" * 60)
    print(title)
    print("=" * 60 + "\n")


def load_admin_password():
    """Load ADMIN_PASSWORD from .env file"""
    env_file = PROJECT_ROOT / ".env"
    if not env_file.exists():
        print(f"  Error: .env file not found at {env_file}")
        sys.exit(1)
    
    with open(env_file, "r") as f:
        for line in f:
            line = line.strip()
            if line.startswith("ADMIN_PASSWORD="):
                password = line.split("=", 1)[1].strip()
                # Remove quotes if present
                password = password.strip('"').strip("'")
                return password
    
    print("  Error: ADMIN_PASSWORD not found in .env file")
    sys.exit(1)


def authenticate_to_production(password):
    """Authenticate to production server using form login and get session cookie"""
    print("Authenticating to production server...")
    # Use form-based login (not JWT) because @JsonIgnore on password field
    # prevents JSON authentication from working
    auth_data = {
        "username": ADMIN_UID,  # Changed from uid
        "password": password
    }
    
    try:
        # Create a session to handle cookies and redirects
        session = requests.Session()
        response = session.post(PROD_LOGIN_URL, data=auth_data, timeout=10, allow_redirects=False)
        
        # Form login returns 302 redirect on success
        if response.status_code == 302 and "sess_java_spring" in response.cookies:
            print(f"  Authenticated as '{ADMIN_UID}'")
            return session.cookies
        else:
            print(f"  Authentication failed: HTTP {response.status_code}")
            print(f"  Response: {response.text[:200]}")
            sys.exit(1)
    except requests.exceptions.HTTPError as e:
        print(f"  Authentication failed: HTTP {e.response.status_code}")
        print(f"  Response: {e.response.text}")
        sys.exit(1)
    except requests.exceptions.RequestException as e:
        print(f"  Error connecting to production server: {e}")
        sys.exit(1)


def is_port_in_use(port):
    """Check if a port is in use"""
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        return s.connect_ex(('localhost', port)) == 0


def fetch_remote_data():
    """Fetch data from remote database with authentication"""
    print("\nFetching remote data...")
    print(f"  Fetching from: {DATA_URL}")
    
    # Load credentials and authenticate
    password = load_admin_password()
    cookies = authenticate_to_production(password)
    
    try:
        headers = {"Content-Type": "application/json"}
        response = requests.get(DATA_URL, headers=headers, cookies=cookies, timeout=30)
        response.raise_for_status()
        
        data = response.json()
        print(f"  Data fetched successfully")
        print(f"  Tables found: {len(data)}")
        
        return data
    except requests.RequestException as e:
        print(f"  Failed to fetch remote data: {e}")
        return None


def save_data_to_json(data):
    """Save data to JSON file with backup"""
    # Backup existing JSON if it exists
    if JSON_DATA_FILE.exists():
        timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
        backup_file = Path(str(JSON_DATA_FILE) + f".{timestamp}.bak")
        shutil.copy2(JSON_DATA_FILE, backup_file)
        print(f"  Existing JSON backed up to {backup_file}")
    
    # Write new data with pretty printing
    with open(JSON_DATA_FILE, 'w') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    
    print(f"Remote data saved to {JSON_DATA_FILE}")


def load_local_json():
    """Load data from local JSON file"""
    if JSON_DATA_FILE.exists():
        try:
            with open(JSON_DATA_FILE, 'r') as f:
                return json.load(f)
        except Exception as e:
            print(f"  Failed to read local JSON: {e}")
    return None


def get_existing_columns(cursor, table_name):
    """Get existing columns from a table"""
    cursor.execute(f'PRAGMA table_info("{table_name}")')
    return {row[1] for row in cursor.fetchall()}  # column name at index 1


def coerce_value(value):
    """Convert unsupported Python types to JSON strings for SQLite"""
    if isinstance(value, (dict, list)):
        return json.dumps(value, ensure_ascii=False)
    return value


def import_data_to_sqlite(data):
    """Import data from JSON into SQLite database"""
    print("\nLoading remote data into new database...")
    print("=" * 60)
    print("Importing Data to SQLite")
    print("=" * 60)
    print()
    
    if not DB_FILE.exists():
        print(f"Error: Database file not found: {DB_FILE}")
        return False
    
    # Connect to database
    print(f"Connecting to database: {DB_FILE}")
    try:
        conn = sqlite3.connect(str(DB_FILE))
        cursor = conn.cursor()
        print("Connected successfully")
    except Exception as e:
        print(f"Error connecting to database: {e}")
        return False
    
    print()
    
    # Get existing tables in database
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")
    existing_tables = {row[0] for row in cursor.fetchall()}
    print(f"Existing tables in database: {len(existing_tables)}")
    
    # Determine allowlist behavior
    env_allow = os.getenv("IMPORT_TABLES")
    allowlist = None  # None means import all tables
    if env_allow is not None:
        token = env_allow.strip()
        if token and token.upper() not in {"*", "ALL"}:
            allowlist = {t.strip() for t in token.split(',') if t.strip()}
    
    if allowlist is None:
        print("Allowlist: ALL tables")
    else:
        print(f"Allowlist: {', '.join(sorted(allowlist))}")
    
    print()
    print("Importing data...")
    print()
    
    # Import data table by table
    imported_count = 0
    skipped_count = 0
    error_count = 0
    
    for table_name, records in data.items():
        # Only import tables in allowlist if provided
        if allowlist is not None and table_name not in allowlist:
            continue
        
        # Skip empty tables
        if not records or len(records) == 0:
            continue
        
        # Check if table exists
        if table_name not in existing_tables:
            print(f"  Skipping {table_name}: table doesn't exist in schema")
            skipped_count += 1
            continue
        
        try:
            # Intersect JSON keys with actual table columns
            table_columns = get_existing_columns(cursor, table_name)
            if not table_columns:
                print(f"  Skipping {table_name}: no columns discovered via PRAGMA")
                skipped_count += 1
                continue
            
            columns = [col for col in records[0].keys() if col in table_columns]
            if not columns:
                print(f"  Skipping {table_name}: no matching columns between JSON and DB")
                skipped_count += 1
                continue
            
            # Prepare insert statement
            placeholders = ','.join(['?' for _ in columns])
            column_names = ','.join([f'"{col}"' for col in columns])
            insert_sql = f'INSERT OR REPLACE INTO "{table_name}" ({column_names}) VALUES ({placeholders})'
            
            # Insert all records
            batch = []
            for record in records:
                values = [coerce_value(record.get(col)) for col in columns]
                batch.append(values)
            cursor.executemany(insert_sql, batch)
            
            conn.commit()
            print(f"  {table_name}: {len(records)} records imported")
            imported_count += 1
            
        except Exception as e:
            print(f"  {table_name}: Error - {str(e)[:120]}")
            error_count += 1
            conn.rollback()
    
    conn.close()
    
    print()
    print("=" * 60)
    print("Import Summary")
    print("=" * 60)
    print(f"  Imported: {imported_count} tables")
    print(f"  Skipped: {skipped_count} tables (not in schema/allowlist)")
    print(f"  Errors: {error_count} tables")
    print()
    
    if imported_count > 0:
        print("Import completed successfully!")
        return True
    else:
        print("No data was imported")
        return False


def backup_database():
    """Backup the current database file"""
    if not DB_FILE.exists():
        print("No existing database file to backup")
        return
    
    # Create backup directory
    BACKUP_DIR.mkdir(parents=True, exist_ok=True)
    
    # Create backup with timestamp
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    backup_file = BACKUP_DIR / f"sqlite_backup_{timestamp}.db"
    
    shutil.copy2(DB_FILE, backup_file)
    print(f"Database backed up to: {backup_file}")
    
    # Backup WAL and SHM files if they exist
    wal_file = Path(str(DB_FILE) + "-wal")
    shm_file = Path(str(DB_FILE) + "-shm")
    
    if wal_file.exists():
        shutil.copy2(wal_file, BACKUP_DIR / f"sqlite_backup_{timestamp}.db-wal")
        print("WAL file backed up")
    
    if shm_file.exists():
        shutil.copy2(shm_file, BACKUP_DIR / f"sqlite_backup_{timestamp}.db-shm")
        print("SHM file backed up")


def remove_database():
    """Remove old database files"""
    print("\nRemoving old database...")
    
    files_to_remove = [
        DB_FILE,
        Path(str(DB_FILE) + "-wal"),
        Path(str(DB_FILE) + "-shm")
    ]
    
    for file in files_to_remove:
        if file.exists():
            file.unlink()
    
    print("Old database removed")


def create_skip_flag():
    """Create flag file to skip ModelInit"""
    SKIP_FLAG_FILE.parent.mkdir(parents=True, exist_ok=True)
    SKIP_FLAG_FILE.touch()
    print(f"Created skip-modelinit flag at {SKIP_FLAG_FILE}")


def remove_skip_flag():
    """Remove the skip flag file"""
    if SKIP_FLAG_FILE.exists():
        SKIP_FLAG_FILE.unlink()
        print("Removed skip-modelinit flag")


def wait_for_spring_boot(timeout=60):
    """Wait for Spring Boot to start"""
    print("Waiting for Spring Boot", end="", flush=True)
    start_time = time.time()
    
    while time.time() - start_time < timeout:
        if is_port_in_use(SPRING_PORT):
            print(" OK")
            time.sleep(3)  # Give it time to complete schema creation
            return True
        print(".", end="", flush=True)
        time.sleep(1)
    
    print("\nTimeout waiting for Spring Boot to start")
    return False


def recreate_schema():
    """Start Spring Boot temporarily to recreate schema"""
    print("\nStarting Spring Boot to recreate schema...")
    print("(ModelInit will be skipped to avoid conflicts)")
    print("(This will take a few seconds...)")
    
    # Create skip flag before starting
    create_skip_flag()
    
    try:
        # Start Spring Boot with ddl-auto=create
        process = subprocess.Popen(
            ["./mvnw", "spring-boot:run", 
             "-Dspring-boot.run.arguments=--spring.jpa.hibernate.ddl-auto=create"],
            stdout=open(LOG_FILE, 'w'),
            stderr=subprocess.STDOUT,
            cwd=PROJECT_ROOT,
            preexec_fn=os.setsid
        )
        
        # Wait for application to start
        if not wait_for_spring_boot():
            print(f"\nApplication failed to start. Check {LOG_FILE} for errors")
            process.terminate()
            sys.exit(1)
        
        # Stop the application
        print("\nStopping temporary instance...")
        try:
            os.killpg(os.getpgid(process.pid), signal.SIGTERM)
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            os.killpg(os.getpgid(process.pid), signal.SIGKILL)
            process.wait()
        
        time.sleep(2)
        print("Temporary instance stopped")
        
    finally:
        # Always remove skip flag
        remove_skip_flag()


def import_remote_data(data):
    """Import remote data directly"""
    return import_data_to_sqlite(data)


def check_spring_boot_running():
    """Check if Spring Boot is already running"""
    if is_port_in_use(SPRING_PORT):
        print(f"WARNING: Spring Boot application is running on port {SPRING_PORT}")
        print("  Please stop it first: pkill -f 'spring-boot:run'")
        sys.exit(1)


def get_user_confirmation():
    """Get user confirmation before proceeding"""
    if not DB_FILE.exists():
        return True
    
    # Check for FORCE_YES environment variable
    if os.getenv('FORCE_YES') == 'true':
        print("FORCE_YES detected, proceeding automatically...")
        return True
    
    print("WARNING: You are about to lose all data in the database!")
    print("WARNING: This operation will:")
    print("   - Backup the current database")
    print("   - Drop all existing tables")
    print("   - Recreate schema from scratch")
    print("   - Import data from remote database")
    
    print()
    response = input("Do you want to continue? (y/n): ").strip().lower()
    return response in ('y', 'yes')


def main():
    """Main migration process"""
    print_header("DATABASE MIGRATION WITH REMOTE IMPORT")
    
    # Step 1: Check if Spring Boot is running
    check_spring_boot_running()
    
    # Step 2: Get user confirmation
    if not get_user_confirmation():
        print("Exiting without making changes.")
        sys.exit(0)
    
    # Step 3: Fetch remote data (now with form-based authentication)
    remote_data = fetch_remote_data()
    if remote_data:
        save_data_to_json(remote_data)
    else:
        print("WARNING: Could not fetch remote data, will use local JSON if available")
        remote_data = load_local_json()
        if not remote_data:
            print("No remote or local data available for import")
            sys.exit(1)
    
    # Step 4: Backup database
    backup_database()
    
    # Step 5: Remove old database
    remove_database()
    
    # Step 6: Recreate schema (with ModelInit skipped)
    recreate_schema()
    
    # Step 7: Import remote data
    import_remote_data(remote_data)
    
    # Success message
    print_header("DATABASE MIGRATION COMPLETE")
    print("Database migrated with:")
    print("  Fresh schema (created by Hibernate)")
    print("  Remote data imported (ModelInit was skipped)")
    print("\nYou can now start your application:")
    print("  ./mvnw spring-boot:run\n")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\nInterrupted by user")
        # Clean up skip flag on interrupt
        if SKIP_FLAG_FILE.exists():
            SKIP_FLAG_FILE.unlink()
        sys.exit(1)
    except Exception as e:
        print(f"\nAn error occurred: {e}", file=sys.stderr)
        # Clean up skip flag on error
        if SKIP_FLAG_FILE.exists():
            SKIP_FLAG_FILE.unlink()
        import traceback
        traceback.print_exc()
        sys.exit(1)