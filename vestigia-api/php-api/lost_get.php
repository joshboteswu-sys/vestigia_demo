<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

$id = $_GET["id"] ?? null;
if (!$id) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Item id is required."]);
    exit;
}

$stmt = $pdo->prepare("SELECT li.*, u.first_name, u.last_name,
                              u.email AS reporter_email, u.contact_number AS reporter_phone
                        FROM lost_items li
                        LEFT JOIN users u ON u.id = li.reported_by
                        WHERE li.id = :id");
$stmt->execute(["id" => $id]);
$item = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$item) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "Item not found."]);
    exit;
}

$protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
$host = $_SERVER['HTTP_HOST'];
$scriptDir = rtrim(dirname($_SERVER['SCRIPT_NAME']), '/');
$baseUrl = $protocol . '://' . $host . $scriptDir . '/';
$item['image_url'] = $item['image_path'] ? $baseUrl . $item['image_path'] : null;

// The reporter's contact details are visible to admins only
$contact = null;
if ($session['is_admin'] && ($item['reporter_email'] || $item['reporter_phone'])) {
    $contact = ["email" => $item['reporter_email'], "phone" => $item['reporter_phone']];
}
unset($item['reporter_email'], $item['reporter_phone']);
$item['contact'] = $contact;

echo json_encode(["success" => true, "item" => $item]);