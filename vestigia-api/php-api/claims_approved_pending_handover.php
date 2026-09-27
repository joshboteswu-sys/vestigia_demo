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

$stmt = $pdo->query("SELECT c.id AS claim_id, c.decision_date, fi.id AS item_id, fi.item_name, fi.location,
                             u.first_name, u.last_name, u.email
                      FROM claims c
                      JOIN found_items fi ON fi.id = c.item_id
                      JOIN users u ON u.id = c.claimed_by
                      WHERE c.status = 'Approved'
                      ORDER BY c.decision_date ASC");
$claims = $stmt->fetchAll(PDO::FETCH_ASSOC);

echo json_encode(["success" => true, "claims" => $claims]);