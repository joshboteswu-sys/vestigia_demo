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

$stmt = $pdo->query("SELECT id, first_name, last_name, email, student_id, is_admin, created_at
                      FROM users ORDER BY created_at DESC");
$users = $stmt->fetchAll(PDO::FETCH_ASSOC);

foreach ($users as &$u) {
    $u['is_admin'] = ($u['is_admin'] === true || $u['is_admin'] === 't' || $u['is_admin'] === '1');
}

echo json_encode(["success" => true, "users" => $users]);