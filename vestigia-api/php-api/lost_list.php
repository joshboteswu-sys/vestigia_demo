<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

$search = trim($_GET["search"] ?? "");
$filter = trim($_GET["filter"] ?? "all");

$sql = "SELECT li.*, u.first_name, u.last_name
        FROM lost_items li
        LEFT JOIN users u ON u.id = li.reported_by
        WHERE 1=1";
$params = [];

if ($search !== "") {
    $sql .= " AND (li.item_name ILIKE :search OR li.last_seen_location ILIKE :search OR li.description ILIKE :search)";
    $params["search"] = "%$search%";
}

if ($filter === "today") {
    $sql .= " AND li.date_lost = CURRENT_DATE";
} elseif ($filter === "week") {
    $sql .= " AND li.date_lost >= CURRENT_DATE - INTERVAL '7 days' AND li.date_lost < CURRENT_DATE";
} elseif ($filter === "older") {
    $sql .= " AND li.date_lost < CURRENT_DATE - INTERVAL '7 days'";
} elseif ($filter === "mine") {
    $sql .= " AND li.reported_by = :mine_id";
    $params["mine_id"] = $session["user_id"];
}

$sql .= " ORDER BY li.date_lost DESC, li.id DESC";

$stmt = $pdo->prepare($sql);
$stmt->execute($params);
$items = $stmt->fetchAll(PDO::FETCH_ASSOC);

$protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
$host = $_SERVER['HTTP_HOST'];
$scriptDir = rtrim(dirname($_SERVER['SCRIPT_NAME']), '/');
$baseUrl = $protocol . '://' . $host . $scriptDir . '/';

foreach ($items as &$item) {
    $item['image_url'] = $item['image_path'] ? $baseUrl . $item['image_path'] : null;
}

echo json_encode(["success" => true, "items" => $items]);