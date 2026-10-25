<?php
require_once "db.php";
require_once "auth_helper.php";
require_once "upload_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

// Multipart/form-data: item_name, category, last_seen_location, date_lost, description, image (file, optional)
$itemName    = trim($_POST['item_name'] ?? '');
$category    = trim($_POST['category'] ?? '');
$location    = trim($_POST['last_seen_location'] ?? '');
$dateLost    = trim($_POST['date_lost'] ?? '');
$description = trim($_POST['description'] ?? '');

if ($itemName === '' || $location === '' || $dateLost === '') {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Item name, last seen location, and date lost are required."]);
    exit;
}

$imagePath = handleImageUpload('image', 'lost_');

try {
    // Status vocabulary is now 'Active' / 'Claimed' / 'Returned' / 'Disposed'
    // — was previously "Missing", which no longer satisfies the current
    // CHECK constraint on lost_items.status and was silently breaking this
    // endpoint with an uncaught DB error.
    $stmt = $pdo->prepare("INSERT INTO lost_items (item_name, description, category, last_seen_location, date_lost, status, image_path, reported_by)
                            VALUES (:name, :desc, :cat, :loc, :date, 'Active', :img, :uid) RETURNING id");
    $stmt->execute([
        "name" => $itemName,
        "desc" => $description,
        "cat"  => $category,
        "loc"  => $location,
        "date" => $dateLost,
        "img"  => $imagePath,
        "uid"  => $session["user_id"]
    ]);
    $newId = $stmt->fetchColumn();

    echo json_encode(["success" => true, "message" => "Lost report submitted.", "item_id" => $newId]);

} catch (PDOException $e) {
    error_log("Vestigia lost_create failed: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not save your lost report right now. Please try again."]);
}