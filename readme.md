# VESTIGIA - Lost and Found Management System

## Overview
--------
Vestigia ("Tracing what's missing") is a lost and found management system 
designed to streamline reporting, claiming, and handing over lost items across 
campus.

## Team Members
------------
* Josh Francis Bote (joshboteswu@phinmaed.com)
* Christhan Jophet Kaquilala Gutang (chka.gutang.swu@phinmaed.com)
* Kharl Adam Latigo (labo.kharladam.swu@phinmaed.com)

## Technology Stack
----------------
* Mobile App (Client) : Android SDK, Java, Volley HTTP library
* API Backend         : PHP 8.3
* Data Access         : PDO (PHP Data Objects)
* Database            : PostgreSQL 18.6 (Wasmer Managed DB)
* Hosting Platform    : Wasmer Edge (wasmer.io)
* Version Control     : Git / GitHub (master branch)

## Setup & Installation Guide
---------------------------

1. Database Setup:
   - Ensure you have a running PostgreSQL 18.6 instance (locally or on Wasmer Edge).
   - Import the database schema and seed data from the provided SQL dump:
     
     psql -U <db_username> -d <db_name> -f vestigia.sql

2. Backend API Setup (PHP 8.3):
   - Clone the repository:
     
     git clone https://github.com/your-org/vestigia.git
     cd vestigia

   - Configure environment connection variables in your hosting environment or .env file:
     * DB_HOST
     * DB_PORT
     * DB_NAME
     * DB_USERNAME
     * DB_PASSWORD

   - Deploy to Wasmer Edge or run via local PHP development server:
     
     php -S localhost:8000

3. Android Mobile Client Setup:
   - Open the project folder in Android Studio.
   - Sync the project with Gradle files (build.gradle).
   - Ensure your API endpoint URL in the Volley request config points to your deployed backend domain or local server IP.
   - Build and run the application on an Android device or emulator.

## Testing & API Documentation
---------------------------
For API testing, endpoint samples, and request payload collections, refer to 
the Postman guide located at:

requirements/README.txt
