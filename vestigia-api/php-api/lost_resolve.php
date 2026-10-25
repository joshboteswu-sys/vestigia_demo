<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

$data = json_decode(file_get_contents("php://input"), true) ?? [];
$id = $data['id'] ?? null;
if (!$id) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Report id is required."]);
    exit;
}

$stmt = $pdo->prepare("SELECT id, reported_by, status FROM lost_items WHERE id = :id");
$stmt->execute(["id" => $id]);
$report = $stmt->fetch(PDO::FETCH_ASSOC);
if (!$report) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "Report not found."]);
    exit;
}

requireOwnerOrAdmin($session, $report['reported_by']);

if ($report['status'] !== 'Active') {
    http_response_code(409);
    echo json_encode(["success" => false, "message" => "This report is already closed."]);
    exit;
}

$pdo->prepare("UPDATE lost_items SET status = 'Returned', updated_at = NOW() WHERE id = :id AND status = 'Active'")
    ->execute(["id" => $id]);

echo json_encode(["success" => true, "message" => "Marked as recovered. Glad you got it back!"]);