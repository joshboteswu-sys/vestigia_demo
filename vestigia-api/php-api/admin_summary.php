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

try {
    $row = $pdo->query("SELECT
            (SELECT COUNT(*) FROM claims WHERE status = 'Pending Verification') AS pending_claims,
            (SELECT COUNT(*) FROM claims WHERE status = 'Approved') AS awaiting_pickup,
            (SELECT COUNT(*) FROM found_items WHERE custody_holder = 'Finder' AND status IN ('Active', 'Claimed')) AS with_finders,
            (SELECT COUNT(*) FROM users) AS total_users")->fetch(PDO::FETCH_ASSOC);

    echo json_encode([
        "success"         => true,
        "pending_claims"  => (int) $row['pending_claims'],
        "awaiting_pickup" => (int) $row['awaiting_pickup'],
        "with_finders"    => (int) $row['with_finders'],
        "total_users"     => (int) $row['total_users'],
    ]);
} catch (Throwable $e) {
    error_log("Vestigia admin_summary failed: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not load the summary."]);
}