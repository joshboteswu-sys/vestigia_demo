<?php
require_once "db.php";
require_once "auth_helper.php";

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

$photoPath = null;
if (isset($_FILES['proof_photo']) && $_FILES['proof_photo']['error'] === UPLOAD_ERR_OK) {
    $uploadsDir = __DIR__ . '/uploads';
    if (!is_dir($uploadsDir)) {
        mkdir($uploadsDir, 0777, true);
    }
    $ext = strtolower(pathinfo($_FILES['proof_photo']['name'], PATHINFO_EXTENSION));
    $allowed = ['jpg', 'jpeg', 'png', 'webp'];
    if (in_array($ext, $allowed)) {
        $filename = uniqid('claim_', true) . '.' . $ext;
        if (move_uploaded_file($_FILES['proof_photo']['tmp_name'], $uploadsDir . '/' . $filename)) {
            $photoPath = 'uploads/' . $filename;
        }
    }
}

$stmt = $pdo->prepare("INSERT INTO claims (item_id, claimed_by, proof_description, proof_photo_path, status)
                        VALUES (:item_id, :uid, :desc, :photo, 'Pending Verification') RETURNING id");
$stmt->execute([
    "item_id" => $itemId,
    "uid"     => $session['user_id'],
    "desc"    => $proofDescription,
    "photo"   => $photoPath
]);
$newId = $stmt->fetchColumn();

echo json_encode(["success" => true, "message" => "Claim submitted. An admin will review it soon.", "claim_id" => $newId]);