<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

if (!$session['is_admin']) {
    http_response_code(403);
    echo json_encode(["success" => false, "message" => "Admins only."]);
    exit;
}

$data = json_decode(file_get_contents("php://input"), true);
$userId = $data['user_id'] ?? null;

if (!$userId) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "User id is required."]);
    exit;
}

if ((int)$userId === (int)$session['user_id']) {
    http_response_code(403);
    echo json_encode(["success" => false, "message" => "You cannot change your own admin status."]);
    exit;
}

$stmt = $pdo->prepare("SELECT is_admin FROM users WHERE id = :id");
$stmt->execute(["id" => $userId]);
$user = $stmt->fetch(PDO::FETCH_ASSOC);
if (!$user) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "User not found."]);
    exit;
}

$currentIsAdmin = ($user['is_admin'] === true || $user['is_admin'] === 't' || $user['is_admin'] === '1');

$update = $pdo->prepare("UPDATE users SET is_admin = :val WHERE id = :id");
$update->execute(["val" => !$currentIsAdmin, "id" => $userId]);

echo json_encode([
    "success"  => true,
    "message"  => $currentIsAdmin ? "Admin access removed." : "User promoted to admin.",
    "is_admin" => !$currentIsAdmin
]);