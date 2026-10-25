<?php
require_once "db.php";
require_once "auth_helper.php";
require_once "notify_helper.php"; // fullName()

$pdo = getDbConnection();
$session = requireAuth($pdo);

$claimId = $_GET['claim_id'] ?? null;
if (!$claimId || !ctype_digit((string) $claimId)) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Claim id is required."]);
    exit;
}

try {
    $stmt = $pdo->prepare("SELECT h.receipt_no, h.pickup_datetime, h.verification_notes, h.staff_id,
                                  c.claimed_by, c.claimed_at, c.decision_date,
                                  fi.item_name, fi.category, fi.location, fi.date_found,
                                  cu.first_name AS cl_first, cu.last_name AS cl_last, cu.student_id AS cl_student,
                                  st.first_name AS st_first, st.last_name AS st_last,
                                  rv.first_name AS rv_first, rv.last_name AS rv_last
                           FROM handovers h
                           JOIN claims c ON c.id = h.claim_id
                           JOIN found_items fi ON fi.id = c.item_id
                           JOIN users cu ON cu.id = c.claimed_by
                           JOIN users st ON st.id = h.staff_id
                           LEFT JOIN users rv ON rv.id = c.reviewed_by
                           WHERE h.claim_id = :id");
    $stmt->execute(["id" => $claimId]);
    $r = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$r) {
        http_response_code(404);
        echo json_encode(["success" => false, "message" => "There is no handover receipt for this claim yet."]);
        exit;
    }

    // Only the admin, the claimant, or whoever handed the item over may see it
    $allowed = $session['is_admin']
        || (int) $r['claimed_by'] === (int) $session['user_id']
        || (int) $r['staff_id'] === (int) $session['user_id'];
    if (!$allowed) {
        http_response_code(403);
        echo json_encode(["success" => false, "message" => "You are not allowed to view this receipt."]);
        exit;
    }

    echo json_encode([
        "success" => true,
        "receipt" => [
            "receipt_no"      => $r['receipt_no'],
            "pickup_datetime" => $r['pickup_datetime'],
            "notes"           => $r['verification_notes'],
            "item_name"       => $r['item_name'],
            "category"        => $r['category'],
            "location"        => $r['location'],
            "date_found"      => $r['date_found'],
            "claimant_name"   => fullName($r['cl_first'], $r['cl_last']),
            "claimant_student_id" => $r['cl_student'],
            "handed_by"       => fullName($r['st_first'], $r['st_last']),
            "approved_by"     => fullName($r['rv_first'], $r['rv_last']),
            "claimed_at"      => $r['claimed_at'],
            "decision_date"   => $r['decision_date'],
        ],
    ]);

} catch (Throwable $e) {
    error_log("Vestigia receipt_get failed: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not load the receipt."]);
}