<?php
require_once "db.php";
require_once "auth_helper.php";
require_once "upload_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

$id          = $_POST['id'] ?? null;
$itemName    = trim($_POST['item_name'] ?? '');
$category    = trim($_POST['category'] ?? '');
$location    = trim($_POST['location'] ?? '');
$dateFound   = trim($_POST['date_found'] ?? '');
$description = trim($_POST['description'] ?? '');
$status      = trim($_POST['status'] ?? 'Active');

if (!$id || $itemName === '' || $location === '' || $dateFound === '') {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Item id, name, location, and date found are required."]);
    exit;
}

$check = $pdo->prepare("SELECT id, reported_by, image_path FROM found_items WHERE id = :id");
$check->execute(["id" => $id]);
$existing = $check->fetch(PDO::FETCH_ASSOC);
if (!$existing) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "Item not found."]);
    exit;
}

requireOwnerOrAdmin($session, $existing['reported_by']);

$imagePath = handleImageUpload('image', 'item_') ?? $existing['image_path'];

$stmt = $pdo->prepare("UPDATE found_items SET
                        item_name = :name, description = :desc, category = :cat,
                        location = :loc, date_found = :date, status = :status,
                        image_path = :img, updated_at = NOW()
                        WHERE id = :id");
$stmt->execute([
    "name"   => $itemName,
    "desc"   => $description,
    "cat"    => $category,
    "loc"    => $location,
    "date"   => $dateFound,
    "status" => $status,
    "img"    => $imagePath,
    "id"     => $id
]);

echo json_encode(["success" => true, "message" => "Item updated successfully."]);