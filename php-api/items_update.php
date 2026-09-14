<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

$data = json_decode(file_get_contents("php://input"), true);

if (empty($data["id"])) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Item id is required."]);
    exit;
}

$check = $pdo->prepare("SELECT id, reported_by FROM found_items WHERE id = :id");
$check->execute(["id" => $data["id"]]);
$existing = $check->fetch(PDO::FETCH_ASSOC);
if (!$existing) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "Item not found."]);
    exit;
}

requireOwnerOrAdmin($session, $existing['reported_by']);

$stmt = $pdo->prepare("UPDATE found_items SET
                        item_name = :name,
                        description = :desc,
                        category = :cat,
                        location = :loc,
                        date_found = :date,
                        status = :status,
                        image_path = COALESCE(:img, image_path),
                        updated_at = NOW()
                        WHERE id = :id");
$stmt->execute([
    "name"   => $data["item_name"],
    "desc"   => $data["description"] ?? null,
    "cat"    => $data["category"] ?? null,
    "loc"    => $data["location"],
    "date"   => $data["date_found"],
    "status" => $data["status"] ?? "Unclaimed",
    "img"    => $data["image_path"] ?? null,
    "id"     => $data["id"]
]);

echo json_encode(["success" => true, "message" => "Item updated successfully."]);