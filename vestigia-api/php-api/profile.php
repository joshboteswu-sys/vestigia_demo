<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);
$userId = $session['user_id'];

// ---- User info ----
$userStmt = $pdo->prepare("SELECT first_name, last_name, email, student_id, contact_number, is_admin, photo_path
                            FROM users WHERE id = :id");
$userStmt->execute(["id" => $userId]);
$user = $userStmt->fetch(PDO::FETCH_ASSOC);
$user['is_admin'] = ($user['is_admin'] === true || $user['is_admin'] === 't' || $user['is_admin'] === '1');

// after: $user = $userStmt->fetch(PDO::FETCH_ASSOC);
$protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
$host = $_SERVER['HTTP_HOST'];
$scriptDir = rtrim(dirname($_SERVER['SCRIPT_NAME']), '/');
$baseUrl = $protocol . '://' . $host . $scriptDir . '/';
$user['photo_url'] = $user['photo_path'] ? $baseUrl . $user['photo_path'] : null;

// ---- Stat counts ----
$lostCountStmt = $pdo->prepare("SELECT COUNT(*) FROM lost_items WHERE reported_by = :id");
$lostCountStmt->execute(["id" => $userId]);
$lostCount = (int)$lostCountStmt->fetchColumn();

$foundCountStmt = $pdo->prepare("SELECT COUNT(*) FROM found_items WHERE reported_by = :id");
$foundCountStmt->execute(["id" => $userId]);
$foundCount = (int)$foundCountStmt->fetchColumn();

$claimedCountStmt = $pdo->prepare("SELECT COUNT(*) FROM claims WHERE claimed_by = :id AND status = 'Approved'");
$claimedCountStmt->execute(["id" => $userId]);
$claimedCount = (int)$claimedCountStmt->fetchColumn();

// ---- Activity lists (most recent 5 each) ----
$lostReportsStmt = $pdo->prepare("SELECT id, item_name, status, date_lost, last_seen_location
                                   FROM lost_items WHERE reported_by = :id
                                   ORDER BY created_at DESC LIMIT 5");
$lostReportsStmt->execute(["id" => $userId]);
$lostReports = $lostReportsStmt->fetchAll(PDO::FETCH_ASSOC);

$foundReportsStmt = $pdo->prepare("SELECT id, item_name, status, date_found, location
                                    FROM found_items WHERE reported_by = :id
                                    ORDER BY created_at DESC LIMIT 5");
$foundReportsStmt->execute(["id" => $userId]);
$foundReports = $foundReportsStmt->fetchAll(PDO::FETCH_ASSOC);

$claimsStmt = $pdo->prepare("SELECT c.id, c.status, c.claimed_at, fi.item_name, fi.location
                              FROM claims c
                              JOIN found_items fi ON fi.id = c.item_id
                              WHERE c.claimed_by = :id
                              ORDER BY c.claimed_at DESC LIMIT 5");
$claimsStmt->execute(["id" => $userId]);
$claims = $claimsStmt->fetchAll(PDO::FETCH_ASSOC);

echo json_encode([
    "success" => true,
    "user" => $user,
    "stats" => [
        "lost_count"    => $lostCount,
        "found_count"   => $foundCount,
        "claimed_count" => $claimedCount
    ],
    "lost_reports"  => $lostReports,
    "found_reports" => $foundReports,
    "claims"        => $claims
]);