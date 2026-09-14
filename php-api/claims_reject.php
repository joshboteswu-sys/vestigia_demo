<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

if (!$session['is_admin']) {
    http_response_code(403);
    echo json_encode(["success" => false, "message" => "Admins only."]);
    exit;
}

$data = json_decode(file_get_contents("php://input"), true);
$claimId = $data['claim_id'] ?? null;
if (!$claimId) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Claim id is required."]);
    exit;
}

$stmt = $pdo->prepare("UPDATE claims SET status = 'Rejected' WHERE id = :id AND status = 'Pending'");
$stmt->execute(["id" => $claimId]);

if ($stmt->rowCount() === 0) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "Pending claim not found."]);
    exit;
}

echo json_encode(["success" => true, "message" => "Claim rejected."]);