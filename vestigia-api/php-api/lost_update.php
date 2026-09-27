<?php
require_once "db.php";
require_once "auth_helper.php";

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

$imagePath = $existing['image_path']; // keep old image unless a new one is uploaded

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
    $filename = uniqid('lost_', true) . '.' . $ext;
    if (move_uploaded_file($_FILES['image']['tmp_name'], $uploadsDir . '/' . $filename)) {
        $imagePath = 'uploads/' . $filename;
    }
}

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
    // Catches any DB-level failure (e.g. an invalid status value) and returns
    // clean JSON instead of letting PHP dump an HTML error page — that raw
    // HTML response is what was making the app report "network error"
    // instead of the real cause.
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not update lost report: " . $e->getMessage()]);
}