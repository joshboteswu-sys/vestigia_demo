<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

$id = $_GET["id"] ?? null;
if (!$id) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Item id is required."]);
    exit;
}

$stmt = $pdo->prepare("SELECT fi.*, u.first_name, u.last_name,
                              u.email AS reporter_email, u.contact_number AS reporter_phone,
                              c.first_name AS custodian_first_name, c.last_name AS custodian_last_name,
                              c.email AS custodian_email, c.contact_number AS custodian_phone
                        FROM found_items fi
                        LEFT JOIN users u ON u.id = fi.reported_by
                        LEFT JOIN users c ON c.id = fi.custodian_id
                        WHERE fi.id = :id");
$stmt->execute(["id" => $id]);
$item = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$item) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "Item not found."]);
    exit;
}

$protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https' : 'http';
$host = $_SERVER['HTTP_HOST'];
$scriptDir = rtrim(dirname($_SERVER['SCRIPT_NAME']), '/');
$baseUrl = $protocol . '://' . $host . $scriptDir . '/';
$item['image_url'] = $item['image_path'] ? $baseUrl . $item['image_path'] : null;

// ---- Contact details are private ----
// Shown only to admins, or to a claimant whose claim was approved on an item a finder still holds.
$isAdmin = $session['is_admin'];
$holderIsFinder = ($item['custody_holder'] ?? 'Finder') === 'Finder';

$hasApprovedClaim = false;
if (!$isAdmin) {
    $q = $pdo->prepare("SELECT 1 FROM claims
                         WHERE item_id = :item AND claimed_by = :uid AND status IN ('Approved', 'Completed') LIMIT 1");
    $q->execute(["item" => $id, "uid" => $session['user_id']]);
    $hasApprovedClaim = (bool) $q->fetchColumn();
}

$contact = null;
if ($isAdmin || ($holderIsFinder && $hasApprovedClaim)) {
    // The person to contact is whoever holds the item (finder custody), otherwise the reporter
    $useCustodian = $holderIsFinder && !empty($item['custodian_id']);
    $email = $useCustodian ? $item['custodian_email'] : $item['reporter_email'];
    $phone = $useCustodian ? $item['custodian_phone'] : $item['reporter_phone'];
    if ($email || $phone) {
        $contact = ["email" => $email, "phone" => $phone];
    }
}

unset($item['reporter_email'], $item['reporter_phone'], $item['custodian_email'], $item['custodian_phone']);
$item['contact'] = $contact;

echo json_encode(["success" => true, "item" => $item]);