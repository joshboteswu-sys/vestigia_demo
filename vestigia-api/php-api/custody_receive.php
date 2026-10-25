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

if ($item['custody_holder'] === 'Admin') {
    http_response_code(409);
    echo json_encode(["success" => false, "message" => "This item is already in Admin custody."]);
    exit;
}

if (in_array($item['status'], ['Returned', 'Disposed'])) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "This item is already closed."]);
    exit;
}

$previousHolder = $item['holder_user_id'];

$pdo->beginTransaction();
try {
    $pdo->prepare("UPDATE found_items
                      SET custody_holder = 'Admin', custodian_id = :admin, custody_note = :note, updated_at = NOW()
                    WHERE id = :id")
        ->execute(["admin" => $session['user_id'], "note" => $note !== '' ? $note : null, "id" => $itemId]);

    $pdo->prepare("INSERT INTO custody_log (item_id, from_holder, to_holder, from_user_id, to_user_id, changed_by, note)
                   VALUES (:item, 'Finder', 'Admin', :from_user, :to_user, :by, :note)")
        ->execute([
            "item" => $itemId, "from_user" => $previousHolder, "to_user" => $session['user_id'],
            "by" => $session['user_id'], "note" => $note !== '' ? $note : "Received from finder"
        ]);

    $pdo->commit();
} catch (Throwable $e) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    error_log("Vestigia: custody_receive failed: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not update custody."]);
    exit;
}

try {
    $adminName = fullName($session['first_name'], $session['last_name']);

    // Thank the finder
    if ($previousHolder && (int) $previousHolder !== (int) $session['user_id']) {
        notifyUser($pdo, $previousHolder, 'custody_received', 'Item received by the Admin Office',
            "$adminName received \"{$item['item_name']}\" into Admin custody. Thank you for turning it in!",
            $itemId);
    }

    // If a claim was already approved, the claimant's pickup place just changed
    $stmt = $pdo->prepare("SELECT id, claimed_by FROM claims WHERE item_id = :item AND status = 'Approved'");
    $stmt->execute(["item" => $itemId]);
    foreach ($stmt->fetchAll(PDO::FETCH_ASSOC) as $c) {
        notifyUser($pdo, $c['claimed_by'], 'pickup_updated', 'Pickup location updated',
            "\"{$item['item_name']}\" is now in the Admin Office. Please collect it there and bring a valid ID.",
            $itemId, $c['id']);
    }
} catch (Throwable $e) {
    error_log("Vestigia: custody_receive notifications failed: " . $e->getMessage());
}

echo json_encode(["success" => true, "message" => "Item received into Admin custody."]);