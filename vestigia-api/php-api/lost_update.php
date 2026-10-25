<?php
require_once "db.php";
require_once "auth_helper.php";
require_once "upload_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

// Multipart/form-data: id, item_name, category, last_seen_location, date_lost, description, status, image (file, optional)
$id          = $_POST['id'] ?? null;
$itemName    = trim($_POST['item_name'] ?? '');
$category    = trim($_POST['category'] ?? '');
$location    = trim($_POST['last_seen_location'] ?? '');
$dateLost    = trim($_POST['date_lost'] ?? '');
$description = trim($_POST['description'] ?? '');
$status      = trim($_POST['status'] ?? 'Active'); // was 'Missing' — no longer a valid status value

if (!$id || $itemName === '' || $location === '' || $dateLost === '') {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Item id, name, location, and date lost are required."]);
    exit;
}

$check = $pdo->prepare("SELECT id, reported_by, image_path FROM lost_items WHERE id = :id");
$check->execute(["id" => $id]);
$existing = $check->fetch(PDO::FETCH_ASSOC);
if (!$existing) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "Item not found."]);
    exit;
}

requireOwnerOrAdmin($session, $existing['reported_by']);

$imagePath = handleImageUpload('image', 'lost_') ?? $existing['image_path'];

try {
    // status must be one of: 'Active', 'Claimed', 'Returned', 'Disposed'
    // (matches the CHECK constraint currently on lost_items.status)
    $stmt = $pdo->prepare("UPDATE lost_items SET
                            item_name = :name, description = :desc, category = :cat,
                            last_seen_location = :loc, date_lost = :date, status = :status,
                            image_path = :img, updated_at = NOW()
                            WHERE id = :id");
    $stmt->execute([
        "name"   => $itemName,
        "desc"   => $description,
        "cat"    => $category,
        "loc"    => $location,
        "date"   => $dateLost,
        "status" => $status,
        "img"    => $imagePath,
        "id"     => $id
    ]);

    echo json_encode(["success" => true, "message" => "Lost report updated."]);

} catch (PDOException $e) {
    error_log("Vestigia lost_update failed: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not update the report right now. Please try again."]);
}