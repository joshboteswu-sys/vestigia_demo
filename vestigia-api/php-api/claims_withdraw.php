<?php
require_once "db.php";
require_once "auth_helper.php";
require_once "notify_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

$data = json_decode(file_get_contents("php://input"), true) ?? [];
$claimId = $data['claim_id'] ?? null;
if (!$claimId) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Claim id is required."]);
    exit;
}

$stmt = $pdo->prepare("SELECT c.id, c.item_id, c.claimed_by, c.status, fi.item_name
                       FROM claims c JOIN found_items fi ON fi.id = c.item_id
                       WHERE c.id = :id");
$stmt->execute(["id" => $claimId]);
$claim = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$claim) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "Claim not found."]);
    exit;
}
if ((int) $claim['claimed_by'] !== (int) $session['user_id']) {
    http_response_code(403);
    echo json_encode(["success" => false, "message" => "You can only withdraw your own claim."]);
    exit;
}
if ($claim['status'] !== 'Pending Verification') {
    http_response_code(409);
    echo json_encode(["success" => false, "message" => "Only a claim that is still pending can be withdrawn."]);
    exit;
}

// The status check in the WHERE clause protects against an admin deciding at the same moment
$upd = $pdo->prepare("UPDATE claims SET status = 'Withdrawn' WHERE id = :id AND claimed_by = :uid AND status = 'Pending Verification'");
$upd->execute(["id" => $claimId, "uid" => $session['user_id']]);
if ($upd->rowCount() === 0) {
    http_response_code(409);
    echo json_encode(["success" => false, "message" => "This claim was just decided and can no longer be withdrawn."]);
    exit;
}

try {
    $who = fullName($session['first_name'], $session['last_name']);
    notifyAdmins($pdo, 'claim_withdrawn', 'Claim withdrawn',
        "$who withdrew their claim for \"{$claim['item_name']}\".",
        $claim['item_id'], $claim['id'], $session['user_id']);
} catch (Throwable $e) {
    error_log("Vestigia claims_withdraw notification failed: " . $e->getMessage());
}

echo json_encode(["success" => true, "message" => "Your claim was withdrawn."]);