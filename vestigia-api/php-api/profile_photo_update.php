<?php
require_once "db.php";
require_once "auth_helper.php";
require_once "upload_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

$photoPath = handleImageUpload('photo', 'user_');
if ($photoPath === null) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "No photo was uploaded."]);
    exit;
}

$stmt = $pdo->prepare("UPDATE users SET photo_path = :photo WHERE id = :id");
$stmt->execute(["photo" => $photoPath, "id" => $session['user_id']]);

$protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
$host = $_SERVER['HTTP_HOST'];
$scriptDir = rtrim(dirname($_SERVER['SCRIPT_NAME']), '/');
$photoUrl = $protocol . '://' . $host . $scriptDir . '/' . $photoPath;

echo json_encode(["success" => true, "message" => "Profile photo updated.", "photo_url" => $photoUrl]);