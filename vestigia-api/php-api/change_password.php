<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

$data = json_decode(file_get_contents("php://input"), true);
$currentPassword = trim($data['current_password'] ?? '');
$newPassword = trim($data['new_password'] ?? '');

if ($currentPassword === '' || $newPassword === '') {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Current and new password are required."]);
    exit;
}

if (strlen($newPassword) < 6) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "New password must be at least 6 characters."]);
    exit;
}

$stmt = $pdo->prepare("SELECT password_hash FROM users WHERE id = :id");
$stmt->execute(["id" => $session['user_id']]);
$user = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$user || !password_verify($currentPassword, $user['password_hash'])) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Current password is incorrect."]);
    exit;
}

$newHash = password_hash($newPassword, PASSWORD_BCRYPT);
$update = $pdo->prepare("UPDATE users SET password_hash = :hash WHERE id = :id");
$update->execute(["hash" => $newHash, "id" => $session['user_id']]);

echo json_encode(["success" => true, "message" => "Password changed successfully."]);