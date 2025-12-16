#!/usr/bin/env python3
"""
db_init.py - Spring Boot Database Initialization

Resets the database to original state with default data.
- Backs up the current database
- Drops all existing tables
- Creates fresh schema via Spring Boot
- Loads default data via ModelInit

Usage:
    cd scripts && ./db_init.py
    or
    scripts/db_init.py
    or with force flag:
    FORCE_YES=true scripts/db_init.py
"""

import os
import sys
import shutil
import subprocess
import signal
import time
import socket
from datetime import datetime
from pathlib import Path

# Configuration
PROJECT_ROOT = Path(__file__).parent.parent
DB_FILE = PROJECT_ROOT / "volumes" / "sqlite.db"
BACKUP_DIR = PROJECT_ROOT / "volumes" / "backups"
SPRING_PORT = 8585
LOG_FILE = "/tmp/db_init.log"


def print_header(title):
    """Print a formatted header"""
    print("\n" + "=" * 60)
    print(title)
    print("=" * 60 + "\n")


def is_port_in_use(port):
    """Check if a port is in use"""
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        return s.connect_ex(('localhost', port)) == 0


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


def wait_for_spring_boot(timeout=60):
    """Wait for Spring Boot to start"""
    print("\nWaiting for Spring Boot to start", end="", flush=True)
    start_time = time.time()
    
    while time.time() - start_time < timeout:
        if is_port_in_use(SPRING_PORT):
            print(" OK")
            time.sleep(3)  # Give ModelInit time to run
            return True
        print(".", end="", flush=True)
        time.sleep(1)
    
    print("\nTimeout waiting for Spring Boot to start")
    return False


def start_spring_boot_temp():
    """Start Spring Boot temporarily to create schema and load data"""
    print("\nStarting Spring Boot to recreate schema and load default data...")
    print("(This will take a few seconds...)")
    
    # Start Spring Boot with ddl-auto=create
    process = subprocess.Popen(
        ["./mvnw", "spring-boot:run", 
         "-Dspring-boot.run.arguments=--spring.jpa.hibernate.ddl-auto=create"],
        stdout=open(LOG_FILE, 'w'),
        stderr=subprocess.STDOUT,
        cwd=PROJECT_ROOT,
        preexec_fn=os.setsid  # Create new process group for clean shutdown
    )
    
    # Wait for application to start
    if not wait_for_spring_boot():
        print(f"\nApplication failed to start. Check {LOG_FILE} for errors")
        process.terminate()
        sys.exit(1)
    
    # Stop the application
    print("\nStopping application...")
    try:
        # Send SIGTERM to process group
        os.killpg(os.getpgid(process.pid), signal.SIGTERM)
        process.wait(timeout=10)
    except subprocess.TimeoutExpired:
        # Force kill if it doesn't stop gracefully
        os.killpg(os.getpgid(process.pid), signal.SIGKILL)
        process.wait()
    
    time.sleep(2)  # Give it time to shutdown cleanly
    print("Application stopped")


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
    print("This will reset the database to original state with default data")
    print("All current data will be lost (after backup)\n")
    
    response = input("Continue? (y/n): ").strip().lower()
    return response in ('y', 'yes')


def main():
    """Main initialization process"""
    print_header("Database Reset to Original State")
    
    # Step 1: Check if Spring Boot is running
    check_spring_boot_running()
    
    # Step 2: Get user confirmation
    if not get_user_confirmation():
        print("Cancelled.")
        sys.exit(0)
    
    # Step 3: Backup database
    backup_database()
    
    # Step 4: Remove old database
    remove_database()
    
    # Step 5: Start Spring Boot to recreate schema and load data
    start_spring_boot_temp()
    
    # Success message
    print_header("DATABASE RESET COMPLETE")
    print("The database has been reset to original state with:")
    print("  Fresh schema created")
    print("  Default data loaded (Person.init(), QuizScore.init(), etc.)")
    print("\nYou can now start your application normally:")
    print("  ./mvnw spring-boot:run\n")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\nInterrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\nAn error occurred: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)