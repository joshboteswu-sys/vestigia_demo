<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
requireAuth($pdo);

$protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
$host = $_SERVER['HTTP_HOST'];
$scriptDir = rtrim(dirname($_SERVER['SCRIPT_NAME']), '/');
$baseUrl = $protocol . '://' . $host . $scriptDir . '/';

$foundStmt = $pdo->query("SELECT fi.id, fi.item_name, fi.location, fi.image_path, fi.created_at,
                                  u.first_name, u.last_name
                           FROM found_items fi
                           LEFT JOIN users u ON u.id = fi.reported_by
                           ORDER BY fi.created_at DESC LIMIT 10");
$foundRows = $foundStmt->fetchAll(PDO::FETCH_ASSOC);

$lostStmt = $pdo->query("SELECT li.id, li.item_name, li.last_seen_location AS location, li.image_path, li.created_at,
                                 u.first_name, u.last_name
                          FROM lost_items li
                          LEFT JOIN users u ON u.id = li.reported_by
                          ORDER BY li.created_at DESC LIMIT 10");
$lostRows = $lostStmt->fetchAll(PDO::FETCH_ASSOC);

$posts = [];
foreach ($foundRows as $r) {
    $posts[] = [
        "id" => $r['id'], "item_type" => "Found", "item_name" => $r['item_name'],
        "location" => $r['location'],
        "image_url" => $r['image_path'] ? $baseUrl . $r['image_path'] : null,
        "reporter_name" => trim($r['first_name'] . ' ' . $r['last_name']),
        "created_at" => $r['created_at']
    ];
}
foreach ($lostRows as $r) {
    $posts[] = [
        "id" => $r['id'], "item_type" => "Lost", "item_name" => $r['item_name'],
        "location" => $r['location'],
        "image_url" => $r['image_path'] ? $baseUrl . $r['image_path'] : null,
        "reporter_name" => trim($r['first_name'] . ' ' . $r['last_name']),
        "created_at" => $r['created_at']
    ];
}

usort($posts, function ($a, $b) {
    return strtotime($b['created_at']) <=> strtotime($a['created_at']);
});

echo json_encode(["success" => true, "posts" => array_slice($posts, 0, 10)]);