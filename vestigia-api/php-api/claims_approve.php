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

$pdo->beginTransaction();
try {
    // Remember the competing claims so we can tell those claimants afterwards
    $others = $pdo->prepare("SELECT id, claimed_by FROM claims
                              WHERE item_id = :item_id AND status = 'Pending Verification' AND id != :id");
    $others->execute(["item_id" => $claim['item_id'], "id" => $claimId]);
    $losers = $others->fetchAll(PDO::FETCH_ASSOC);

    $pdo->prepare("UPDATE claims SET status = 'Approved', reviewed_by = :admin, decision_date = NOW() WHERE id = :id")
        ->execute(["admin" => $session['user_id'], "id" => $claimId]);

    $pdo->prepare("UPDATE found_items SET status = 'Claimed', updated_at = NOW() WHERE id = :id")
        ->execute(["id" => $claim['item_id']]);

    $pdo->prepare("UPDATE claims SET status = 'Rejected', reviewed_by = :admin, decision_date = NOW(),
                          rejection_feedback = 'Item was awarded to another claimant.'
                    WHERE item_id = :item_id AND status = 'Pending Verification' AND id != :id")
        ->execute(["admin" => $session['user_id'], "item_id" => $claim['item_id'], "id" => $claimId]);

    $pdo->commit();
} catch (Exception $e) {
    $pdo->rollBack();
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not approve claim."]);
    exit;
}

// ---- Notifications (after the commit; a failure here never undoes the approval) ----
try {
    $item     = getItemCustody($pdo, $claim['item_id']);
    $claimant = getUserBasic($pdo, $claim['claimed_by']);
    $itemName = $item['item_name'] ?? 'the item';
    $claimantName = $claimant ? fullName($claimant['first_name'], $claimant['last_name']) : 'the claimant';

    // 1) The approved claimant: where to pick it up (finder contact is shared only now)
    notifyUser($pdo, $claim['claimed_by'], 'claim_approved', 'Your claim was approved',
        "Good news! Your claim for \"$itemName\" was approved. Please collect it from "
        . custodyDescription($item, true) . ". Bring a valid ID so the handover can be confirmed.",
        $claim['item_id'], $claim['id']);

    // 2) A finder who still holds the item: hand it over and record it
    if ($item && $item['custody_holder'] === 'Finder' && $item['holder_user_id']
        && (int) $item['holder_user_id'] !== (int) $session['user_id']) {
        $contact = implode(', ', array_filter([$claimant['email'] ?? '', $claimant['contact_number'] ?? '']));
        notifyUser($pdo, $item['holder_user_id'], 'handover_due', 'Please hand over the item',
            "A claim for \"$itemName\" was approved. Please hand the item to $claimantName"
            . ($contact !== '' ? " ($contact)" : "")
            . " and then record it under Pending handovers in the app.",
            $claim['item_id'], $claim['id']);
    }

    // 3) Competing claimants who were auto-rejected
    foreach ($losers as $loser) {
        notifyUser($pdo, $loser['claimed_by'], 'claim_rejected', 'Claim not approved',
            "Your claim for \"$itemName\" was not approved because the item was awarded to another claimant.",
            $claim['item_id'], $loser['id']);
    }
} catch (Throwable $e) {
    error_log("Vestigia: claims_approve notifications failed: " . $e->getMessage());
}

echo json_encode(["success" => true, "message" => "Claim approved. The claimant has been notified."]);