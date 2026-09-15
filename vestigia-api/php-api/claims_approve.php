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
$claimId = $data['claim_id'] ?? null;
if (!$claimId) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Claim id is required."]);
    exit;
}

$stmt = $pdo->prepare("SELECT id, item_id FROM claims WHERE id = :id AND status = 'Pending'");
$stmt->execute(["id" => $claimId]);
$claim = $stmt->fetch(PDO::FETCH_ASSOC);
if (!$claim) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "Pending claim not found."]);
    exit;
}

$pdo->beginTransaction();
try {
    $pdo->prepare("UPDATE claims SET status = 'Approved' WHERE id = :id")->execute(["id" => $claimId]);
    $pdo->prepare("UPDATE found_items SET status = 'Claimed', updated_at = NOW() WHERE id = :id")->execute(["id" => $claim['item_id']]);
    // Auto-reject any other pending claims on the same item
    $pdo->prepare("UPDATE claims SET status = 'Rejected' WHERE item_id = :item_id AND status = 'Pending' AND id != :id")
        ->execute(["item_id" => $claim['item_id'], "id" => $claimId]);
    $pdo->commit();
} catch (Exception $e) {
    $pdo->rollBack();
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not approve claim."]);
    exit;
}

echo json_encode(["success" => true, "message" => "Claim approved. Item marked as claimed."]);