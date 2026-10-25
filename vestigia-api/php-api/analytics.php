<?php
require_once "db.php";
require_once "auth_helper.php";

// ---- Retention policy behind the "Expiring soon" card (edit to match your office's rules) ----
const RETENTION_DAYS      = 30; // how long an unclaimed item is held before it may be disposed of
const EXPIRY_WARNING_DAYS = 7;  // "expiring soon" = within this many days of that limit

$pdo = getDbConnection();
$session = requireAuth($pdo);

if (!$session['is_admin']) {
    http_response_code(403);
    echo json_encode(["success" => false, "message" => "Admins only."]);
    exit;
}

$range = $_GET['range'] ?? '30d';
if (!in_array($range, ['7d', '30d', '12m'], true)) $range = '30d';

try {
    // ---- Card 1: return rate ----
    $items = $pdo->query("SELECT COUNT(*) AS total,
                                 COUNT(*) FILTER (WHERE status = 'Returned') AS returned
                          FROM found_items")->fetch(PDO::FETCH_ASSOC);
    $totalItems    = (int) $items['total'];
    $returnedItems = (int) $items['returned'];
    $returnRate    = $totalItems > 0 ? round($returnedItems / $totalItems * 100, 1) : 0;

    // ---- Card 2: active claims + users ----
    $claims = $pdo->query("SELECT
            COUNT(*) FILTER (WHERE status = 'Pending Verification') AS pending,
            COUNT(*) FILTER (WHERE status = 'Approved') AS awaiting_pickup,
            COUNT(*) FILTER (WHERE status IN ('Approved', 'Completed')) AS approved_total,
            COUNT(*) FILTER (WHERE status = 'Rejected'
                              AND rejection_feedback IS DISTINCT FROM 'Item was awarded to another claimant.') AS rejected_total
        FROM claims")->fetch(PDO::FETCH_ASSOC);

    $totalUsers = (int) $pdo->query("SELECT COUNT(*) FROM users")->fetchColumn();
    $stmt = $pdo->prepare("SELECT COUNT(DISTINCT user_id) FROM sessions WHERE expires_at > :now");
    $stmt->execute(["now" => date("Y-m-d H:i:s")]);
    $activeUsers = (int) $stmt->fetchColumn();

    // ---- Card 3: custody with finders ----
    $custodyFinder = (int) $pdo->query("SELECT COUNT(*) FROM found_items
                                        WHERE custody_holder = 'Finder' AND status IN ('Active', 'Claimed')")->fetchColumn();

    // ---- Card 4: expiring soon ----
    $warnStart = RETENTION_DAYS - EXPIRY_WARNING_DAYS;
    $retention = RETENTION_DAYS;
    $exp = $pdo->query("SELECT
            COUNT(*) FILTER (WHERE (CURRENT_DATE - date_found) BETWEEN $warnStart AND $retention) AS expiring,
            COUNT(*) FILTER (WHERE (CURRENT_DATE - date_found) > $retention) AS overdue
        FROM found_items WHERE status = 'Active'")->fetch(PDO::FETCH_ASSOC);

    // ---- Line graph: reported vs returned over time ----
    if ($range === '12m') {
        $sql = "SELECT to_char(g.m, 'Mon YY') AS label,
                       (SELECT COUNT(*) FROM found_items f WHERE date_trunc('month', f.created_at) = g.m) AS reported,
                       (SELECT COUNT(*) FROM handovers h WHERE date_trunc('month', h.pickup_datetime) = g.m) AS returned
                FROM generate_series(date_trunc('month', CURRENT_DATE::timestamp) - INTERVAL '11 months',
                                     date_trunc('month', CURRENT_DATE::timestamp),
                                     INTERVAL '1 month') AS g(m)
                ORDER BY g.m";
    } else {
        $days = ($range === '7d') ? 6 : 29;
        $sql = "SELECT to_char(g.d, 'Mon DD') AS label,
                       (SELECT COUNT(*) FROM found_items f WHERE f.created_at::date = g.d::date) AS reported,
                       (SELECT COUNT(*) FROM handovers h WHERE h.pickup_datetime::date = g.d::date) AS returned
                FROM generate_series((CURRENT_DATE - $days)::timestamp, CURRENT_DATE::timestamp, INTERVAL '1 day') AS g(d)
                ORDER BY g.d";
    }
    $labels = [];
    $reported = [];
    $returned = [];
    foreach ($pdo->query($sql)->fetchAll(PDO::FETCH_ASSOC) as $row) {
        $labels[]   = $row['label'];
        $reported[] = (int) $row['reported'];
        $returned[] = (int) $row['returned'];
    }

    // ---- Donut: status + custody breakdown ----
    $seg = $pdo->query("SELECT
            COUNT(*) FILTER (WHERE status = 'Active' AND custody_holder = 'Finder') AS active_finder,
            COUNT(*) FILTER (WHERE status = 'Active' AND custody_holder = 'Admin')  AS active_admin,
            COUNT(*) FILTER (WHERE status = 'Claimed')  AS claimed,
            COUNT(*) FILTER (WHERE status = 'Returned') AS returned,
            COUNT(*) FILTER (WHERE status = 'Disposed') AS disposed
        FROM found_items")->fetch(PDO::FETCH_ASSOC);

    $breakdown = [
        ["key" => "active_finder", "label" => "Active - with finder",       "count" => (int) $seg['active_finder']],
        ["key" => "active_admin",  "label" => "Active - Admin Office",      "count" => (int) $seg['active_admin']],
        ["key" => "claimed",       "label" => "Claimed - awaiting pickup",  "count" => (int) $seg['claimed']],
        ["key" => "returned",      "label" => "Returned",                   "count" => (int) $seg['returned']],
        ["key" => "disposed",      "label" => "Disposed",                   "count" => (int) $seg['disposed']],
    ];

    // ---- Approval vs rejection ----
    $approved = (int) $claims['approved_total'];
    $rejected = (int) $claims['rejected_total'];
    $decided  = $approved + $rejected;

    echo json_encode([
        "success" => true,
        "range"   => $range,
        "cards"   => [
            "return_rate"    => ["percent" => $returnRate, "returned" => $returnedItems, "total" => $totalItems],
            "active_claims"  => [
                "count"           => (int) $claims['pending'] + (int) $claims['awaiting_pickup'],
                "pending"         => (int) $claims['pending'],
                "awaiting_pickup" => (int) $claims['awaiting_pickup'],
                "active_users"    => $activeUsers,
                "total_users"     => $totalUsers,
            ],
            "custody_finder" => ["count" => $custodyFinder],
            "expiring_soon"  => [
                "count"          => (int) $exp['expiring'],
                "overdue"        => (int) $exp['overdue'],
                "retention_days" => RETENTION_DAYS,
                "warning_days"   => EXPIRY_WARNING_DAYS,
            ],
        ],
        "timeline"  => ["labels" => $labels, "reported" => $reported, "returned" => $returned],
        "breakdown" => $breakdown,
        "decisions" => [
            "approved"      => $approved,
            "rejected"      => $rejected,
            "pending"       => (int) $claims['pending'],
            "approval_rate" => $decided > 0 ? round($approved / $decided * 100, 1) : 0,
        ],
    ]);

} catch (Throwable $e) {
    error_log("Vestigia analytics failed: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not load analytics."]);
}