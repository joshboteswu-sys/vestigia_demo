<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
requireAuth($pdo);

$id = $_GET["id"] ?? null;
if (!$id) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Item id is required."]);
    exit;
}

$stmt = $pdo->prepare("SELECT fi.*, u.first_name, u.last_name
                        FROM found_items fi
                        LEFT JOIN users u ON u.id = fi.reported_by
                        WHERE fi.id = :id");
$stmt->execute(["id" => $id]);
$item = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$item) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "Item not found."]);
    exit;
}

$protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
$host = $_SERVER['HTTP_HOST'];
$scriptDir = rtrim(dirname($_SERVER['SCRIPT_NAME']), '/');
$baseUrl = $protocol . '://' . $host . $scriptDir . '/';
$item['image_url'] = $item['image_path'] ? $baseUrl . $item['image_path'] : null;

echo json_encode(["success" => true, "item" => $item]);