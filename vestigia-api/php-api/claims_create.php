<?php
require_once "db.php";
require_once "auth_helper.php";
require_once "notify_helper.php";
require_once "upload_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

$itemId = $_POST['item_id'] ?? null;
$proofDescription = trim($_POST['proof_description'] ?? '');

if (!$itemId || $proofDescription === '') {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Please describe your proof of ownership."]);
    exit;
}

$itemStmt = $pdo->prepare("SELECT id, status, reported_by FROM found_items WHERE id = :id");
$itemStmt->execute(["id" => $itemId]);
$item = $itemStmt->fetch(PDO::FETCH_ASSOC);

if (!$item) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "Item not found."]);
    exit;
}

if ($item['status'] !== 'Active') {
    http_response_code(409);
    echo json_encode(["success" => false, "message" => "This item is no longer open for claims."]);
    exit;
}

if ((int)$item['reported_by'] === (int)$session['user_id']) {
    http_response_code(403);
    echo json_encode(["success" => false, "message" => "You cannot claim an item you reported yourself."]);
    exit;
}

$dupCheck = $pdo->prepare("SELECT id FROM claims WHERE item_id = :item_id AND claimed_by = :uid AND status = 'Pending Verification'");
$dupCheck->execute(["item_id" => $itemId, "uid" => $session['user_id']]);
if ($dupCheck->fetch()) {
    http_response_code(409);
    echo json_encode(["success" => false, "message" => "You already have a pending claim on this item."]);
    exit;
}

$photoPath = handleImageUpload('proof_photo', 'claim_');

$stmt = $pdo->prepare("INSERT INTO claims (item_id, claimed_by, proof_description, proof_photo_path, status)
                        VALUES (:item_id, :uid, :desc, :photo, 'Pending Verification') RETURNING id");
$stmt->execute([
    "item_id" => $itemId,
    "uid"     => $session['user_id'],
    "desc"    => $proofDescription,
    "photo"   => $photoPath
]);
  $newId = $stmt->fetchColumn();

  // ---- Notify whoever holds the item (and the admins who must review the claim) ----
  try {
      $item         = getItemCustody($pdo, $itemId);
      $claimant     = getUserBasic($pdo, $session['user_id']);
      $claimantName = $claimant ? fullName($claimant['first_name'], $claimant['last_name']) : 'Someone';
      $itemName     = $item['item_name'] ?? 'an item';

      if ($item && $item['custody_holder'] === 'Finder') {
          // The finder still has the item: they should keep it safe until the claim is reviewed
          if ($item['holder_user_id'] && (int) $item['holder_user_id'] !== (int) $session['user_id']) {
              notifyUser($pdo, $item['holder_user_id'], 'claim_submitted', 'New claim on your found item',
                  "$claimantName submitted a claim for \"$itemName\", which you are currently holding. "
                  . "Please keep it safe until an administrator reviews the claim. You will be told the result.",
                  $itemId, $newId);
          }
          notifyAdmins($pdo, 'claim_submitted', 'Claim awaiting review',
              "$claimantName submitted a claim for \"$itemName\". The item is currently held by "
              . custodyDescription($item, true) . ". Review it under Admin Panel > Review pending claims.",
              $itemId, $newId, $session['user_id']);
      } else {
          notifyAdmins($pdo, 'claim_submitted', 'New claim on an item in the Admin Office',
              "$claimantName submitted a claim for \"$itemName\", which is in the Admin Office. "
              . "Review it under Admin Panel > Review pending claims.",
              $itemId, $newId, $session['user_id']);
      }
  } catch (Throwable $e) {
      error_log("Vestigia: claim notifications failed: " . $e->getMessage());
  }

  echo json_encode(["success" => true, "message" => "Claim submitted. The person holding the item and an admin have been notified.", "claim_id" => $newId]);