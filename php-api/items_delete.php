<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

$data = json_decode(file_get_contents("php://input"), true);
$id = $data["id"] ?? ($_GET["id"] ?? null);

if (!$id) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Item id is required."]);
    exit;
}

$check = $pdo->prepare("SELECT id, reported_by FROM found_items WHERE id = :id");
$check->execute(["id" => $id]);
$existing = $check->fetch(PDO::FETCH_ASSOC);
if (!$existing) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "Item not found."]);
    exit;
}

requireOwnerOrAdmin($session, $existing['reported_by']);

$stmt = $pdo->prepare("DELETE FROM found_items WHERE id = :id");
$stmt->execute(["id" => $id]);

echo json_encode(["success" => true, "message" => "Item deleted successfully."]);