<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

$search = trim($_GET["search"] ?? "");
$filter = trim($_GET["filter"] ?? "all");

$sql = "SELECT fi.*, u.first_name, u.last_name
        FROM found_items fi
        LEFT JOIN users u ON u.id = fi.reported_by
        WHERE 1=1";
$params = [];

if ($search !== "") {
    $sql .= " AND (fi.item_name ILIKE :search OR fi.location ILIKE :search OR fi.description ILIKE :search)";
    $params["search"] = "%$search%";
}

if ($filter === "today") {
    $sql .= " AND fi.date_found = CURRENT_DATE";
} elseif ($filter === "week") {
    $sql .= " AND fi.date_found >= CURRENT_DATE - INTERVAL '7 days' AND fi.date_found < CURRENT_DATE";
} elseif ($filter === "older") {
    $sql .= " AND fi.date_found < CURRENT_DATE - INTERVAL '7 days'";
} elseif ($filter === "mine") {
    $sql .= " AND fi.reported_by = :mine_id";
    $params["mine_id"] = $session["user_id"];
}

$sql .= " ORDER BY fi.date_found DESC, fi.id DESC";

$stmt = $pdo->prepare($sql);
$stmt->execute($params);
$items = $stmt->fetchAll(PDO::FETCH_ASSOC);

// Build a full image URL for each item so Android can load it directly
$protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
$host = $_SERVER['HTTP_HOST'];
$scriptDir = rtrim(dirname($_SERVER['SCRIPT_NAME']), '/');
$baseUrl = $protocol . '://' . $host . $scriptDir . '/';

foreach ($items as &$item) {
    $item['image_url'] = $item['image_path'] ? $baseUrl . $item['image_path'] : null;
}

echo json_encode(["success" => true, "items" => $items]);