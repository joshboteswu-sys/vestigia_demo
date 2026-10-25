<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

$data = json_decode(file_get_contents("php://input"), true) ?? [];

if (!empty($data['all'])) {
    $pdo->prepare("UPDATE notifications SET is_read = TRUE WHERE user_id = :uid AND is_read = FALSE")
        ->execute(["uid" => $session['user_id']]);
} elseif (!empty($data['id'])) {
    $pdo->prepare("UPDATE notifications SET is_read = TRUE WHERE id = :id AND user_id = :uid")
        ->execute(["id" => $data['id'], "uid" => $session['user_id']]);
} else {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Notification id is required."]);
    exit;
}

echo json_encode(["success" => true]);