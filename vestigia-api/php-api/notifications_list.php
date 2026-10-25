<?php
require_once "db.php";
require_once "auth_helper.php";
require_once "verification_helper.php"; // dbBool()

$pdo = getDbConnection();
$session = requireAuth($pdo);

$limit = (int) ($_GET['limit'] ?? 50);
if ($limit < 1) $limit = 1;
if ($limit > 100) $limit = 100;

$countStmt = $pdo->prepare("SELECT COUNT(*) FROM notifications WHERE user_id = :uid AND is_read = FALSE");
$countStmt->execute(["uid" => $session['user_id']]);
$unread = (int) $countStmt->fetchColumn();

$stmt = $pdo->prepare("SELECT id, type, title, message, item_id, claim_id, is_read, created_at
                       FROM notifications
                       WHERE user_id = :uid
                       ORDER BY created_at DESC, id DESC
                       LIMIT :limit");
$stmt->bindValue(':uid', (int) $session['user_id'], PDO::PARAM_INT);
$stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
$stmt->execute();
$rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

foreach ($rows as &$r) {
    $r['is_read'] = dbBool($r['is_read']);
}

echo json_encode(["success" => true, "unread_count" => $unread, "notifications" => $rows]);