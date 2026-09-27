<?php
require_once "db.php";
require_once "auth_helper.php";

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

$imagePath = $existing['image_path'];

if (isset($_FILES['image']) && $_FILES['image']['error'] === UPLOAD_ERR_OK) {
    $uploadsDir = __DIR__ . '/uploads';
    if (!is_dir($uploadsDir)) {
        mkdir($uploadsDir, 0777, true);
    }
    $ext = strtolower(pathinfo($_FILES['image']['name'], PATHINFO_EXTENSION));
    $allowed = ['jpg', 'jpeg', 'png', 'webp'];
    if (!in_array($ext, $allowed)) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "Only JPG, PNG, or WEBP images are allowed."]);
        exit;
    }
    $filename = uniqid('item_', true) . '.' . $ext;
    if (move_uploaded_file($_FILES['image']['tmp_name'], $uploadsDir . '/' . $filename)) {
        $imagePath = 'uploads/' . $filename;
    }
}

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