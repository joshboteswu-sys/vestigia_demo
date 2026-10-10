<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

if (!isset($_FILES['photo']) || $_FILES['photo']['error'] !== UPLOAD_ERR_OK) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "No photo was uploaded."]);
    exit;
}

$ext = strtolower(pathinfo($_FILES['photo']['name'], PATHINFO_EXTENSION));
$allowed = ['jpg', 'jpeg', 'png', 'webp'];
if (!in_array($ext, $allowed)) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Only JPG, PNG, or WEBP images are allowed."]);
    exit;
}

$uploadsDir = __DIR__ . '/uploads';
if (!is_dir($uploadsDir)) {
    mkdir($uploadsDir, 0777, true);
}

$filename = uniqid('user_', true) . '.' . $ext;
if (!move_uploaded_file($_FILES['photo']['tmp_name'], $uploadsDir . '/' . $filename)) {
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Failed to save uploaded photo."]);
    exit;
}

$photoPath = 'uploads/' . $filename;

$stmt = $pdo->prepare("UPDATE users SET photo_path = :photo WHERE id = :id");
$stmt->execute(["photo" => $photoPath, "id" => $session['user_id']]);

$protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
$host = $_SERVER['HTTP_HOST'];
$scriptDir = rtrim(dirname($_SERVER['SCRIPT_NAME']), '/');
$photoUrl = $protocol . '://' . $host . $scriptDir . '/' . $photoPath;

echo json_encode(["success" => true, "message" => "Profile photo updated.", "photo_url" => $photoUrl]);