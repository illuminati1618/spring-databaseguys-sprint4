#!/usr/bin/env python3
"""
mysqlbackup.py - MySQL to SQLite Backup

Backs up all data from MySQL server and stores it locally in a SQLite backup file.
Reads MySQL connection details from .env file.

Usage:
    python3 scripts/mysqlbackup.py
    cd scripts && python3 mysqlbackup.py
"""

import os
import sys
import re
import sqlite3
import mysql.connector
from mysql.connector import Error
from datetime import datetime
from pathlib import Path

# Configuration
# Script is in scripts/ folder, so go up one level to get project root
PROJECT_ROOT = Path(__file__).parent.parent
BACKUP_DIR = PROJECT_ROOT / "volumes" / "backups"
BACKUP_DIR.mkdir(parents=True, exist_ok=True)
ENV_FILE = PROJECT_ROOT / ".env"


def load_env_file():
    """Load environment variables from .env file"""
    env_vars = {}
    
    if not ENV_FILE.exists():
        print(f"Error: .env file not found at {ENV_FILE}")
        sys.exit(1)
    
    with open(ENV_FILE, 'r') as f:
        for line in f:
            line = line.strip()
            # Skip comments and empty lines
            if not line or line.startswith('#'):
                continue
            
            # Parse KEY=VALUE format
            if '=' in line:
                key, value = line.split('=', 1)
                key = key.strip()
                value = value.strip()
                # Remove quotes if present
                value = value.strip('"').strip("'")
                env_vars[key] = value
    
    return env_vars


def parse_jdbc_url(jdbc_url):
    """Parse JDBC URL format: jdbc:mysql://host:port/database"""
    # Pattern: jdbc:mysql://host:port/database
    pattern = r'jdbc:mysql://([^:/]+):(\d+)/(.+)'
    match = re.match(pattern, jdbc_url)
    
    if not match:
        print(f"Error: Invalid JDBC URL format: {jdbc_url}")
        print("Expected format: jdbc:mysql://host:port/database")
        sys.exit(1)
    
    host = match.group(1)
    port = int(match.group(2))
    database = match.group(3)
    
    return host, port, database


def get_mysql_config():
    """Get MySQL configuration from .env file"""
    env_vars = load_env_file()
    
    # Required variables
    db_url = env_vars.get('DB_URL')
    db_username = env_vars.get('DB_USERNAME')
    db_password = env_vars.get('DB_PASSWORD')
    
    if not db_url:
        print("Error: DB_URL not found in .env file")
        sys.exit(1)
    
    if not db_username:
        print("Error: DB_USERNAME not found in .env file")
        sys.exit(1)
    
    if not db_password:
        print("Error: DB_PASSWORD not found in .env file")
        sys.exit(1)
    
    # Parse JDBC URL
    host, port, database = parse_jdbc_url(db_url)
    
    return host, port, db_username, db_password, database


def print_header(title):
    """Print a formatted header"""
    print("\n" + "=" * 60)
    print(title)
    print("=" * 60 + "\n")


def get_mysql_connection(host, port, user, password, database):
    """Create MySQL connection"""
    try:
        connection = mysql.connector.connect(
            host=host,
            port=port,
            user=user,
            password=password,
            database=database
        )
        return connection
    except Error as e:
        print(f"Error connecting to MySQL: {e}")
        sys.exit(1)


def get_table_names(mysql_conn):
    """Get all table names from MySQL database and ensure they are decoded strings. (FIXED)"""
    cursor = mysql_conn.cursor()
    cursor.execute("SHOW TABLES")
    
    tables = []
    for row in cursor.fetchall():
        table_name = row[0]
        
        # FIX: Decode bytes/bytearray table names into standard strings
        if isinstance(table_name, (bytes, bytearray)):
            try:
                table_name = table_name.decode('utf-8')
            except UnicodeDecodeError as e:
                print(f"Warning: Could not decode table name {row[0]}: {e}. Skipping table.")
                continue 
        
        tables.append(table_name)
    
    cursor.close()
    return tables


def get_table_schema(mysql_conn, table_name):
    """Get CREATE TABLE statement for a table"""
    cursor = mysql_conn.cursor()
    cursor.execute(f"SHOW CREATE TABLE `{table_name}`")
    result = cursor.fetchone()
    cursor.close()
    if result:
        return result[1]
    return None


def adapt_mysql_to_sqlite_schema(mysql_schema):
    """Adapt MySQL CREATE TABLE statement to SQLite-compatible format (FINAL FIX)"""
    # Basic adaptations
    sqlite_schema = mysql_schema
    
    # Replace MySQL-specific types with SQLite equivalents
    replacements = [
        (r'\bTINYINT\b', 'INTEGER'),
        (r'\bSMALLINT\b', 'INTEGER'),
        (r'\bMEDIUMINT\b', 'INTEGER'),
        (r'\bINT\b', 'INTEGER'),
        (r'\bBIGINT\b', 'INTEGER'),
        (r'\bTINYTEXT\b', 'TEXT'),
        (r'\bMEDIUMTEXT\b', 'TEXT'),
        (r'\bLONGTEXT\b', 'TEXT'),
        (r'\bDATETIME\b', 'TEXT'),
        # --- CRITICAL FIX: Removed the generic (r'\bTIMESTAMP\b', 'TEXT') replacement
        # --- as it was causing the column name 'timestamp' to be replaced with 'TEXT'.
        (r'\bJSON\b', 'TEXT'),
        (r'\bJSONB\b', 'TEXT'),
        (r'\bBLOB\b', 'BLOB'),
        (r'\bLONGBLOB\b', 'BLOB'),

        # FIX: Robust conversion for ENUM, SET, VARCHAR/CHAR to handle length specifiers
        (r'\b(ENUM|SET)\s*\(.*?\)', 'TEXT'),  # Converts ENUM/SET and its options to TEXT
        (r'\bVARCHAR\s*\(\s*\d+\s*\)', 'TEXT'), # Converts VARCHAR(N) to TEXT
        (r'\bCHAR\s*\(\s*\d+\s*\)', 'TEXT'),    # Converts CHAR(N) to TEXT
        (r'\bTINYBLOB\b', 'BLOB'),
    ]
    
    import re
    for pattern, replacement in replacements:
        sqlite_schema = re.sub(pattern, replacement, sqlite_schema, flags=re.IGNORECASE)
    
    # Remove MySQL-specific options
    sqlite_schema = re.sub(r'ENGINE=\w+', '', sqlite_schema, flags=re.IGNORECASE)
    sqlite_schema = re.sub(r'DEFAULT CHARSET=\w+', '', sqlite_schema, flags=re.IGNORECASE)
    sqlite_schema = re.sub(r'COLLATE=\w+', '', sqlite_schema, flags=re.IGNORECASE)
    sqlite_schema = re.sub(r'AUTO_INCREMENT=\d+', '', sqlite_schema, flags=re.IGNORECASE)
    
    # Remove collation sequences from column definitions (e.g., COLLATE utf8mb4_unicode_ci)
    sqlite_schema = re.sub(r'\s+COLLATE\s+[`\'"]?[a-zA-Z0-9_\-\.]+[`\'"]?', '', sqlite_schema, flags=re.IGNORECASE)
    
    # Remove character set specifications from column definitions (e.g., CHARACTER SET utf8mb4)
    sqlite_schema = re.sub(r'\s+CHARACTER\s+SET\s+[`\'"]?[a-zA-Z0-9_\-\.]+[`\'"]?', '', sqlite_schema, flags=re.IGNORECASE)
    sqlite_schema = re.sub(r'\s+CHARSET\s+[`\'"]?[a-zA-Z0-9_\-\.]+[`\'"]?', '', sqlite_schema, flags=re.IGNORECASE)
    
    # Multiple passes for cleanup
    for _ in range(3):
        sqlite_schema = re.sub(r'\s+COLLATE\s+[a-zA-Z0-9_\-\.]+', '', sqlite_schema, flags=re.IGNORECASE)
    
    # Remove trailing commas before closing parenthesis
    sqlite_schema = re.sub(r',\s*\)', ')', sqlite_schema)
    
    # Clean up multiple spaces and normalize whitespace
    sqlite_schema = re.sub(r'\s+', ' ', sqlite_schema)
    sqlite_schema = sqlite_schema.strip()
    
    return sqlite_schema


def copy_table_data(mysql_conn, sqlite_conn, table_name):
    """Copy data from MySQL table to SQLite table"""
    mysql_cursor = mysql_conn.cursor()
    sqlite_cursor = sqlite_conn.cursor()
    
    try:
        # Fetch all data from MySQL
        mysql_cursor.execute(f"SELECT * FROM `{table_name}`")
        columns = [desc[0] for desc in mysql_cursor.description]
        
        # Get all rows
        rows = mysql_cursor.fetchall()
        
        if not rows:
            print(f"  Table '{table_name}': 0 rows (empty)")
            return
        
        # Create placeholders for INSERT statement
        placeholders = ','.join(['?' for _ in columns])
        columns_str = ','.join([f'`{col}`' for col in columns])
        
        # Insert data into SQLite
        insert_sql = f"INSERT INTO `{table_name}` ({columns_str}) VALUES ({placeholders})"
        
        sqlite_cursor.executemany(insert_sql, rows)
        sqlite_conn.commit()
        
        print(f"  Table '{table_name}': {len(rows)} rows copied")
        
    except Exception as e:
        print(f"  Error copying table '{table_name}': {e}")
        sqlite_conn.rollback()
    finally:
        mysql_cursor.close()


def backup_mysql_to_sqlite(host, port, user, password, database, backup_file):
    """Backup MySQL database to SQLite file"""
    print_header("MySQL Backup to SQLite")
    
    # Connect to MySQL
    print(f"Connecting to MySQL: {user}@{host}:{port}/{database}")
    mysql_conn = get_mysql_connection(host, port, user, password, database)
    
    # Create SQLite backup file
    print(f"\nCreating SQLite backup file: {backup_file}")
    if backup_file.exists():
        backup_file.unlink()
    
    sqlite_conn = sqlite3.connect(str(backup_file))
    
    try:
        # Get all tables
        print("\nFetching table list...")
        tables = get_table_names(mysql_conn)
        print(f"Found {len(tables)} tables")
        
        # Copy schema and data for each table
        print("\nCopying tables...")
        for table_name in tables:
            print(f"\nProcessing table: {table_name}")
            
            # Get MySQL schema
            mysql_schema = get_table_schema(mysql_conn, table_name)
            if not mysql_schema:
                print(f"  Warning: Could not get schema for '{table_name}', skipping")
                continue
            
            # Adapt schema for SQLite
            sqlite_schema = adapt_mysql_to_sqlite_schema(mysql_schema)
            
            # Create table in SQLite
            try:
                sqlite_cursor = sqlite_conn.cursor()
                sqlite_cursor.execute(f"DROP TABLE IF EXISTS `{table_name}`")
                sqlite_cursor.execute(sqlite_schema)
                sqlite_conn.commit()
                sqlite_cursor.close()
            except Exception as e:
                print(f"  Error creating table '{table_name}' in SQLite: {e}")
                # Provide debug info if a potential schema issue remains
                if 'collate' in sqlite_schema.lower():
                    print(f"  WARNING: Collation sequence still found in schema!")
                    collate_pos = sqlite_schema.lower().find('collate')
                    start = max(0, collate_pos - 50)
                    end = min(len(sqlite_schema), collate_pos + 100)
                    print(f"  Schema snippet around collation: ...{sqlite_schema[start:end]}...")
                continue
            
            # Copy data
            copy_table_data(mysql_conn, sqlite_conn, table_name)
        
        print_header("Backup Complete")
        print(f"Backup saved to: {backup_file}")
        print(f"Total tables backed up: {len(tables)}")
        
    finally:
        mysql_conn.close()
        sqlite_conn.close()


def main():
    """Main backup process"""
    # Get MySQL configuration from .env
    host, port, username, password, database = get_mysql_config()
    
    # Generate backup filename with timestamp
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    backup_file = BACKUP_DIR / f"mysql_backup_{timestamp}.db"
    
    try:
        backup_mysql_to_sqlite(
            host,
            port,
            username,
            password,
            database,
            backup_file
        )
    except KeyboardInterrupt:
        print("\n\nBackup interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\nAn error occurred: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()