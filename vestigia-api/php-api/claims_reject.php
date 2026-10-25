<?php
require_once "db.php";
require_once "auth_helper.php";
require_once "notify_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

if (!$session['is_admin']) {
    http_response_code(403);
    echo json_encode(["success" => false, "message" => "Admins only."]);
    exit;
}

$data = json_decode(file_get_contents("php://input"), true) ?? [];
$claimId = $data['claim_id'] ?? null;
$feedback = trim($data['rejection_feedback'] ?? '');

if (!$claimId) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Claim id is required."]);
    exit;
}

$stmt = $pdo->prepare("SELECT id, item_id, claimed_by FROM claims WHERE id = :id AND status = 'Pending Verification'");
$stmt->execute(["id" => $claimId]);
$claim = $stmt->fetch(PDO::FETCH_ASSOC);
if (!$claim) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "Pending claim not found."]);
    exit;
}

$pdo->prepare("UPDATE claims SET status = 'Rejected', reviewed_by = :admin, decision_date = NOW(), rejection_feedback = :feedback
                WHERE id = :id")
    ->execute([
        "admin"    => $session['user_id'],
        "feedback" => $feedback !== '' ? $feedback : null,
        "id"       => $claimId
    ]);

try {
    $item = getItemCustody($pdo, $claim['item_id']);
    $itemName = $item['item_name'] ?? 'the item';
    $reason = $feedback !== '' ? $feedback : 'No reason was given.';

    notifyUser($pdo, $claim['claimed_by'], 'claim_rejected', 'Claim not approved',
        "Your claim for \"$itemName\" was not approved.\nReason: $reason\n"
        . "If the item is still listed, you can submit a new claim with more detail.",
        $claim['item_id'], $claim['id']);
} catch (Throwable $e) {
    error_log("Vestigia: claims_reject notifications failed: " . $e->getMessage());
}

echo json_encode(["success" => true, "message" => "Claim rejected. The claimant has been notified."]);