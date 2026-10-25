<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

$sql = "SELECT c.id AS claim_id, c.decision_date, fi.id AS item_id, fi.item_name, fi.location, fi.custody_holder,
               u.first_name, u.last_name, u.email
        FROM claims c
        JOIN found_items fi ON fi.id = c.item_id
        JOIN users u ON u.id = c.claimed_by
        WHERE c.status = 'Approved'";
$params = [];

if (!$session['is_admin']) {
    // A finder only sees approved claims on items they are still holding
    $sql .= " AND fi.custody_holder = 'Finder' AND COALESCE(fi.custodian_id, fi.reported_by) = :uid";
    $params["uid"] = $session['user_id'];
}

$sql .= " ORDER BY c.decision_date ASC";

$stmt = $pdo->prepare($sql);
$stmt->execute($params);

echo json_encode(["success" => true, "claims" => $stmt->fetchAll(PDO::FETCH_ASSOC)]);