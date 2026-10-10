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
$notes = trim($data['verification_notes'] ?? '');

if (!$claimId) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Claim id is required."]);
    exit;
}

$claimStmt = $pdo->prepare("SELECT id, item_id FROM claims WHERE id = :id AND status = 'Approved'");
$claimStmt->execute(["id" => $claimId]);
$claim = $claimStmt->fetch(PDO::FETCH_ASSOC);
if (!$claim) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "An approved claim with this id was not found, or it already has a handover."]);
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

echo json_encode(["success" => true, "message" => "Handover recorded successfully.", "receipt_no" => $receiptNo]);