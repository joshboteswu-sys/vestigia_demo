<?php
// Enable full error display for debugging
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

// ============================================================
// db.php - PostgreSQL connection (PDO)
// ============================================================
$DB_HOST = "localhost";
$DB_PORT = "5432";
$DB_NAME = "vestigia";
$DB_USER = "postgres";
$DB_PASS = "root";

function getDbConnection() {
    global $DB_HOST, $DB_PORT, $DB_NAME, $DB_USER, $DB_PASS;
    try {
        $dsn = "pgsql:host=$DB_HOST;port=$DB_PORT;dbname=$DB_NAME";
        $pdo = new PDO($dsn, $DB_USER, $DB_PASS);
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        return $pdo;
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Database connection failed: " . $e->getMessage()]);
        exit;
    }
}

// Common headers
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

