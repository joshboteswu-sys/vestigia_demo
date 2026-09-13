<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

// Now expects multipart/form-data: item_name, category, location, date_found, description, image (file, optional)
$itemName    = trim($_POST['item_name'] ?? '');
$category    = trim($_POST['category'] ?? '');
$location    = trim($_POST['location'] ?? '');
$dateFound   = trim($_POST['date_found'] ?? '');
$description = trim($_POST['description'] ?? '');

if ($itemName === '' || $location === '' || $dateFound === '') {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Item name, location, and date found are required."]);
    exit;
}

$imagePath = null;
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
    if (!move_uploaded_file($_FILES['image']['tmp_name'], $uploadsDir . '/' . $filename)) {
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Failed to save uploaded image."]);
        exit;
    }
    $imagePath = 'uploads/' . $filename;
}

$stmt = $pdo->prepare("INSERT INTO found_items (item_name, description, category, location, date_found, status, image_path, reported_by)
                        VALUES (:name, :desc, :cat, :loc, :date, 'Unclaimed', :img, :uid) RETURNING id");
$stmt->execute([
    "name" => $itemName,
    "desc" => $description,
    "cat"  => $category,
    "loc"  => $location,
    "date" => $dateFound,
    "img"  => $imagePath,
    "uid"  => $session["user_id"]
]);
$newId = $stmt->fetchColumn();

echo json_encode(["success" => true, "message" => "Item added successfully.", "item_id" => $newId]);