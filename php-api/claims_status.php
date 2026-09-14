<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

$itemId = $_GET['item_id'] ?? null;
if (!$itemId) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Item id is required."]);
    exit;
}

$stmt = $pdo->prepare("SELECT id, status, proof_description, claimed_at
                        FROM claims
                        WHERE item_id = :item_id AND claimed_by = :uid
                        ORDER BY claimed_at DESC LIMIT 1");
$stmt->execute(["item_id" => $itemId, "uid" => $session['user_id']]);
$claim = $stmt->fetch(PDO::FETCH_ASSOC);

echo json_encode(["success" => true, "claim" => $claim ?: null]);