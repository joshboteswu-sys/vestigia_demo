<?php
// ============================================================
// db.php - PostgreSQL connection (PDO)
// Edit these five values to match your pgAdmin4 / Postgres setup
// ============================================================
$DB_HOST = "localhost";
$DB_PORT = "5432";
$DB_NAME = "vestigia";      // the database you created in pgAdmin4
$DB_USER = "postgres";      // your postgres username
$DB_PASS = "root"; // your postgres password

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

// Common headers for every endpoint
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}
