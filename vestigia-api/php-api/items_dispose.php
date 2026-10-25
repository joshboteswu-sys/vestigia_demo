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

$data   = json_decode(file_get_contents("php://input"), true) ?? [];
$itemId = $data['item_id'] ?? null;
$note   = trim($data['note'] ?? '');

if (!$itemId) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Item id is required."]);
    exit;
}

$item = getItemCustody($pdo, $itemId);
if (!$item) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "Item not found."]);
    exit;
}
if ($item['status'] !== 'Active') {
    http_response_code(409);
    echo json_encode(["success" => false,
        "message" => "Only unclaimed (Active) items can be disposed of. This one is " . $item['status'] . "."]);
    exit;
}

$pending = $pdo->prepare("SELECT id, claimed_by FROM claims WHERE item_id = :item AND status = 'Pending Verification'");
$pending->execute(["item" => $itemId]);
$pendingClaims = $pending->fetchAll(PDO::FETCH_ASSOC);

$pdo->beginTransaction();
try {
    $pdo->prepare("UPDATE claims SET status = 'Rejected', reviewed_by = :admin, decision_date = NOW(),
                          rejection_feedback = 'This item has been disposed of.'
                    WHERE item_id = :item AND status = 'Pending Verification'")
        ->execute(["admin" => $session['user_id'], "item" => $itemId]);

    $pdo->prepare("UPDATE found_items SET status = 'Disposed', updated_at = NOW() WHERE id = :id AND status = 'Active'")
        ->execute(["id" => $itemId]);

    $pdo->prepare("INSERT INTO custody_log (item_id, from_holder, to_holder, from_user_id, changed_by, note)
                   VALUES (:item, :from_holder, 'Disposed', :from_user, :by, :note)")
        ->execute([
            "item" => $itemId, "from_holder" => $item['custody_holder'],
            "from_user" => $item['holder_user_id'], "by" => $session['user_id'],
            "note" => substr($note !== '' ? $note : 'Disposed of by an administrator', 0, 255),
        ]);

    $pdo->commit();
} catch (Throwable $e) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    error_log("Vestigia items_dispose failed: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not mark the item as disposed."]);
    exit;
}

try {
    $reason = $note !== '' ? "\nReason: $note" : '';

    foreach ($pendingClaims as $c) {
        notifyUser($pdo, $c['claimed_by'], 'claim_rejected', 'Claim not approved',
            "Your claim for \"{$item['item_name']}\" was closed because the item has been disposed of.",
            $itemId, $c['id']);
    }

    if ($item['holder_user_id'] && (int) $item['holder_user_id'] !== (int) $session['user_id']) {
        notifyUser($pdo, $item['holder_user_id'], 'item_disposed', 'Item marked as disposed',
            "\"{$item['item_name']}\" was marked as disposed of by an administrator because it went unclaimed.$reason",
            $itemId);
    }
} catch (Throwable $e) {
    error_log("Vestigia items_dispose notifications failed: " . $e->getMessage());
}

echo json_encode(["success" => true, "message" => "Item marked as disposed."]);