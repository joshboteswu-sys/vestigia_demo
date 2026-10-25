<?php
require_once "db.php";
require_once "auth_helper.php";
require_once "notify_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

$data = json_decode(file_get_contents("php://input"), true) ?? [];
$claimId = $data['claim_id'] ?? null;
$notes = trim($data['verification_notes'] ?? '');

if (!$claimId) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Claim id is required."]);
    exit;
}

$claimStmt = $pdo->prepare("SELECT c.id, c.item_id, c.claimed_by, fi.item_name, fi.custody_holder, fi.custodian_id, fi.reported_by
                            FROM claims c
                            JOIN found_items fi ON fi.id = c.item_id
                            WHERE c.id = :id AND c.status = 'Approved'");
$claimStmt->execute(["id" => $claimId]);
$claim = $claimStmt->fetch(PDO::FETCH_ASSOC);
if (!$claim) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "An approved claim with this id was not found, or it already has a handover."]);
    exit;
}

// Who may record the handover: any admin, or the finder who is holding the item
$holderId = ($claim['custody_holder'] === 'Finder') ? ($claim['custodian_id'] ?: $claim['reported_by']) : null;
$isCustodian = $holderId !== null && (int) $holderId === (int) $session['user_id'];
if (!$session['is_admin'] && !$isCustodian) {
    http_response_code(403);
    echo json_encode(["success" => false, "message" => "Only an admin or the person holding the item can record the handover."]);
    exit;
}

$dupCheck = $pdo->prepare("SELECT id FROM handovers WHERE claim_id = :id");
$dupCheck->execute(["id" => $claimId]);
if ($dupCheck->fetch()) {
    http_response_code(409);
    echo json_encode(["success" => false, "message" => "This claim already has a recorded handover."]);
    exit;
}

$receiptNo = "RCPT-" . strtoupper(bin2hex(random_bytes(4)));

$pdo->beginTransaction();
try {
    // staff_id holds whoever physically handed the item over (admin or finder)
    $pdo->prepare("INSERT INTO handovers (claim_id, staff_id, receipt_no, verification_notes)
                    VALUES (:claim_id, :staff, :receipt, :notes)")
        ->execute([
            "claim_id" => $claimId,
            "staff"    => $session['user_id'],
            "receipt"  => $receiptNo,
            "notes"    => $notes !== '' ? $notes : null
        ]);

    $pdo->prepare("UPDATE claims SET status = 'Completed' WHERE id = :id")
        ->execute(["id" => $claimId]);

    $pdo->prepare("UPDATE found_items SET status = 'Returned', updated_at = NOW() WHERE id = :id")
        ->execute(["id" => $claim['item_id']]);

    $pdo->commit();
} catch (Exception $e) {
    $pdo->rollBack();
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not record handover."]);
    exit;
}

try {
    notifyUser($pdo, $claim['claimed_by'], 'handover_done', 'Item handed over',
        "\"{$claim['item_name']}\" was handed over to you. Your receipt number is $receiptNo. Keep it for your records.",
        $claim['item_id'], $claim['id']);
} catch (Throwable $e) {
    error_log("Vestigia: handover notification failed: " . $e->getMessage());
}

echo json_encode(["success" => true, "message" => "Handover recorded successfully.", "receipt_no" => $receiptNo]);