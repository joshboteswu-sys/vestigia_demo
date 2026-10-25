<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
requireAuth($pdo);

$type = strtolower(trim($_GET['type'] ?? 'all'));          // all | found | lost
if (!in_array($type, ['all', 'found', 'lost'], true)) $type = 'all';
$search   = trim($_GET['search'] ?? '');
$category = trim($_GET['category'] ?? '');
$custody  = trim($_GET['custody'] ?? '');                  // finder_held (admin view)

$protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
$host = $_SERVER['HTTP_HOST'];
$scriptDir = rtrim(dirname($_SERVER['SCRIPT_NAME']), '/');
$baseUrl = $protocol . '://' . $host . $scriptDir . '/';

// Every value gets its own placeholder, so the same text can safely be used in both halves of the UNION
$params = [];
$n = 0;
$bind = function ($value) use (&$params, &$n) {
    $n++;
    $key = ':p' . $n;
    $params[$key] = $value;
    return $key;
};

$parts = [];

// ---- Found items (only open ones: Active, or Claimed and awaiting pickup) ----
if ($type !== 'lost') {
    $where = ["fi.status IN ('Active', 'Claimed')"];
    if ($custody === 'finder_held') $where[] = "fi.custody_holder = 'Finder'";
    if ($search !== '') {
        $like = '%' . $search . '%';
        $where[] = "(fi.item_name ILIKE " . $bind($like) . " OR fi.location ILIKE " . $bind($like)
                 . " OR fi.description ILIKE " . $bind($like) . " OR fi.category ILIKE " . $bind($like) . ")";
    }
    if ($category !== '') $where[] = "fi.category ILIKE " . $bind('%' . $category . '%');

    $parts[] = "SELECT 'Found'::text AS item_type, fi.id, fi.item_name, fi.location::text AS location,
                       fi.category, fi.status, fi.image_path, fi.created_at, u.first_name, u.last_name
                FROM found_items fi
                LEFT JOIN users u ON u.id = fi.reported_by
                WHERE " . implode(' AND ', $where);
}

// ---- Lost reports (custody only exists for found items) ----
if ($type !== 'found' && $custody !== 'finder_held') {
    $where = ["li.status = 'Active'"];
    if ($search !== '') {
        $like = '%' . $search . '%';
        $where[] = "(li.item_name ILIKE " . $bind($like) . " OR li.last_seen_location ILIKE " . $bind($like)
                 . " OR li.description ILIKE " . $bind($like) . " OR li.category ILIKE " . $bind($like) . ")";
    }
    if ($category !== '') $where[] = "li.category ILIKE " . $bind('%' . $category . '%');

    $parts[] = "SELECT 'Lost'::text AS item_type, li.id, li.item_name, li.last_seen_location::text AS location,
                       li.category, li.status, li.image_path, li.created_at, u.first_name, u.last_name
                FROM lost_items li
                LEFT JOIN users u ON u.id = li.reported_by
                WHERE " . implode(' AND ', $where);
}

if (!$parts) {
    echo json_encode(["success" => true, "posts" => []]);
    exit;
}

try {
    $sql = "SELECT * FROM (" . implode(" UNION ALL ", $parts) . ") p
            ORDER BY created_at DESC, id DESC
            LIMIT 200";
    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    $posts = [];
    foreach ($rows as $r) {
        $posts[] = [
            "id"            => (int) $r['id'],
            "item_type"     => $r['item_type'],
            "item_name"     => $r['item_name'],
            "location"      => $r['location'],
            "category"      => $r['category'],
            "status"        => $r['status'],
            "image_url"     => $r['image_path'] ? $baseUrl . $r['image_path'] : null,
            "reporter_name" => trim(($r['first_name'] ?? '') . ' ' . ($r['last_name'] ?? '')),
            "created_at"    => $r['created_at'],
        ];
    }

    echo json_encode(["success" => true, "posts" => $posts]);

} catch (Throwable $e) {
    error_log("Vestigia posts_list failed: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not load posts."]);
}