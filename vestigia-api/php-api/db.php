<?php
// ============================================================
// db.php - PostgreSQL connection (PDO)
// Edit these five values to match your pgAdmin4 / Postgres setup
// ============================================================
$envPath = dirname(__DIR__, 2) . DIRECTORY_SEPARATOR . '.env';
$envConfig = [];

if (is_file($envPath)) {
    $parsedEnv = parse_ini_file($envPath, false, INI_SCANNER_RAW);
    if ($parsedEnv === false) {
        throw new RuntimeException('Unable to parse the project .env file.');
    }
    $envConfig = $parsedEnv;
}

$getConfigValue = static function (string $name, ?string $default = null) use ($envConfig): ?string {
    $environmentValue = getenv($name);
    if ($environmentValue !== false && $environmentValue !== '') {
        return $environmentValue;
    }

    $fileValue = $envConfig[$name] ?? null;
    return is_string($fileValue) && $fileValue !== '' ? $fileValue : $default;
};

$DB_HOST = $getConfigValue('DB_HOST', 'localhost');
$DB_PORT = $getConfigValue('DB_PORT', '5432');
$DB_NAME = $getConfigValue('DB_NAME', 'vestigia');
$DB_USER = $getConfigValue('DB_USER');
$DB_PASS = $getConfigValue('DB_PASS');

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
