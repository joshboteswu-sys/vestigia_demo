<?php
require_once "db.php";
require_once "auth_helper.php";
require_once "notify_helper.php"; // fullName(), getItemCustody(), dbBool()

$pdo = getDbConnection();
$session = requireAuth($pdo);

$claimId = $_GET['id'] ?? null;
if (!$claimId || !ctype_digit((string) $claimId)) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Claim id is required."]);
    exit;
}

try {
    $stmt = $pdo->prepare("SELECT c.*, cu.first_name AS cl_first, cu.last_name AS cl_last,
                                  cu.email AS cl_email, cu.contact_number AS cl_phone,
                                  rv.first_name AS rv_first, rv.last_name AS rv_last
                           FROM claims c
                           JOIN users cu ON cu.id = c.claimed_by
                           LEFT JOIN users rv ON rv.id = c.reviewed_by
                           WHERE c.id = :id");
    $stmt->execute(["id" => $claimId]);
    $claim = $stmt->fetch(PDO::FETCH_ASSOC);
    if (!$claim) {
        http_response_code(404);
        echo json_encode(["success" => false, "message" => "Claim not found."]);
        exit;
    }

    $custody = getItemCustody($pdo, $claim['item_id']);
    if (!$custody) {
        http_response_code(404);
        echo json_encode(["success" => false, "message" => "The item for this claim no longer exists."]);
        exit;
    }

    // ---- Who may open this claim ----
    $isAdmin    = $session['is_admin'];
    $isClaimant = (int) $claim['claimed_by'] === (int) $session['user_id'];
    $isCustodian = $custody['custody_holder'] === 'Finder'
                && $custody['holder_user_id']
                && (int) $custody['holder_user_id'] === (int) $session['user_id'];
    $decided = in_array($claim['status'], ['Approved', 'Completed'], true);

    if (!$isAdmin && !$isClaimant && !($isCustodian && $decided)) {
        http_response_code(403);
        echo json_encode(["success" => false, "message" => "You are not allowed to view this claim."]);
        exit;
    }

    // ---- Item extras ----
    $itemStmt = $pdo->prepare("SELECT category, location, image_path FROM found_items WHERE id = :id");
    $itemStmt->execute(["id" => $claim['item_id']]);
    $extra = $itemStmt->fetch(PDO::FETCH_ASSOC) ?: [];

    $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
    $host = $_SERVER['HTTP_HOST'];
    $scriptDir = rtrim(dirname($_SERVER['SCRIPT_NAME']), '/');
    $baseUrl = $protocol . '://' . $host . $scriptDir . '/';
    $img = function ($p) use ($baseUrl) { return $p ? $baseUrl . $p : null; };

    // ---- Contact rules ----
    $showClaimantContact = $isAdmin || ($isCustodian && $decided);
    $showHolderContact   = $custody['custody_holder'] === 'Finder' && ($isAdmin || ($isClaimant && $decided));

    // ---- Handover (only exists once completed) ----
    $handover = null;
    $h = $pdo->prepare("SELECT h.receipt_no, h.pickup_datetime, h.verification_notes, s.first_name, s.last_name
                        FROM handovers h JOIN users s ON s.id = h.staff_id
                        WHERE h.claim_id = :id");
    $h->execute(["id" => $claimId]);
    $hr = $h->fetch(PDO::FETCH_ASSOC);
    if ($hr) {
        $handover = [
            "receipt_no"      => $hr['receipt_no'],
            "pickup_datetime" => $hr['pickup_datetime'],
            "notes"           => $hr['verification_notes'],
            "handed_by"       => fullName($hr['first_name'], $hr['last_name']),
        ];
    }

    echo json_encode([
        "success" => true,
        "claim" => [
            "id"                 => (int) $claim['id'],
            "status"             => $claim['status'],
            "claimed_at"         => $claim['claimed_at'],
            "proof_description"  => $claim['proof_description'],
            "proof_photo_url"    => $img($claim['proof_photo_path']),
            "rejection_feedback" => $claim['rejection_feedback'],
            "decision_date"      => $claim['decision_date'],
            "reviewer_name"      => fullName($claim['rv_first'], $claim['rv_last']),
        ],
        "item" => [
            "id"        => (int) $custody['id'],
            "item_name" => $custody['item_name'],
            "status"    => $custody['status'],
            "category"  => $extra['category'] ?? null,
            "location"  => $extra['location'] ?? null,
            "image_url" => $img($extra['image_path'] ?? null),
        ],
        "claimant" => [
            "name"  => fullName($claim['cl_first'], $claim['cl_last']),
            "email" => $showClaimantContact ? $claim['cl_email'] : null,
            "phone" => $showClaimantContact ? $claim['cl_phone'] : null,
        ],
        "custody" => [
            "holder" => $custody['custody_holder'],
            "name"   => $custody['holder_name'],
            "note"   => $custody['custody_note'],
            "email"  => $showHolderContact ? $custody['holder_email'] : null,
            "phone"  => $showHolderContact ? $custody['holder_phone'] : null,
        ],
        "handover" => $handover,
        "viewer" => [
            "is_admin"     => (bool) $isAdmin,
            "is_claimant"  => $isClaimant,
            "is_custodian" => $isCustodian,
        ],
    ]);

} catch (Throwable $e) {
    error_log("Vestigia claims_get failed: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not load the claim."]);
}