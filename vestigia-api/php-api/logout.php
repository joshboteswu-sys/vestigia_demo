<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
$token = getBearerToken();

if (!$token) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "No active session found."]);
    exit;
}

$stmt = $pdo->prepare("DELETE FROM sessions WHERE token = :token");
$stmt->execute(["token" => $token]);

echo json_encode(["success" => true, "message" => "Logged out successfully."]);
