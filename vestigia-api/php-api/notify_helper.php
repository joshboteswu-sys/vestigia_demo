<?php
// ============================================================
// notify_helper.php — in-app + email notifications, and custody lookups
// ============================================================
require_once "verification_helper.php"; // brings in mailer.php (sendEmail) and dbBool()

function fullName($first, $last) {
    return trim(($first ?? '') . ' ' . ($last ?? ''));
}

function getUserBasic($pdo, $userId) {
    $stmt = $pdo->prepare("SELECT id, first_name, last_name, email, contact_number FROM users WHERE id = :id");
    $stmt->execute(["id" => $userId]);
    return $stmt->fetch(PDO::FETCH_ASSOC) ?: null;
}

function notificationEmailContent($firstName, $title, $message) {
    $text = "Hi $firstName,\n\n$message\n\n- Vestigia";

    $name      = htmlspecialchars($firstName, ENT_QUOTES, 'UTF-8');
    $safeTitle = htmlspecialchars($title, ENT_QUOTES, 'UTF-8');
    $safeMsg   = nl2br(htmlspecialchars($message, ENT_QUOTES, 'UTF-8'));
    $html = '<div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;padding:24px;color:#0F172A">'
          . '<h2 style="color:#0F766E;margin:0 0 4px">VESTIGIA</h2>'
          . '<p style="color:#64748B;margin:0 0 24px">Tracing what\'s missing</p>'
          . "<p>Hi $name,</p>"
          . "<h3 style=\"margin:16px 0 8px\">$safeTitle</h3>"
          . "<p style=\"line-height:1.5\">$safeMsg</p>"
          . '<p style="color:#64748B;font-size:13px;margin-top:24px">Open the Vestigia app to see the details.</p>'
          . '</div>';

    return [$html, $text];
}

// Saves an in-app notification and, when the user prefers email, emails it too.
// Never throws: a notification problem must never break the action that triggered it.
function notifyUser($pdo, $userId, $type, $title, $message, $itemId = null, $claimId = null) {
    if (!$userId) return;
    try {
        $stmt = $pdo->prepare("SELECT first_name, email, preferred_contact, email_verified FROM users WHERE id = :id");
        $stmt->execute(["id" => $userId]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);
        if (!$user) return;

        $pdo->prepare("INSERT INTO notifications (user_id, type, title, message, item_id, claim_id)
                       VALUES (:uid, :type, :title, :message, :item, :claim)")
            ->execute([
                "uid" => $userId, "type" => $type, "title" => $title,
                "message" => $message, "item" => $itemId, "claim" => $claimId
            ]);

        // Email only for users who chose Email (or never chose). SMS/Push users get in-app only.
        $pref = $user['preferred_contact'];
        $wantsEmail = ($pref === null || $pref === '' || $pref === 'Email');
        if ($wantsEmail && dbBool($user['email_verified'])) {
            list($html, $text) = notificationEmailContent($user['first_name'], $title, $message);
            sendEmail($pdo, $user['email'], "Vestigia: $title", $html, $text);
        }
    } catch (Throwable $e) {
        error_log("Vestigia: notifyUser failed: " . $e->getMessage());
    }
}

function notifyAdmins($pdo, $type, $title, $message, $itemId = null, $claimId = null, $excludeUserId = null) {
    try {
        $ids = $pdo->query("SELECT id FROM users WHERE is_admin = TRUE")->fetchAll(PDO::FETCH_COLUMN);
    } catch (Throwable $e) {
        error_log("Vestigia: notifyAdmins failed: " . $e->getMessage());
        return;
    }
    foreach ($ids as $id) {
        if ($excludeUserId !== null && (int) $id === (int) $excludeUserId) continue;
        notifyUser($pdo, $id, $type, $title, $message, $itemId, $claimId);
    }
}

// Returns the item with who holds it: holder_user_id, holder_name, holder_email, holder_phone.
// Finder custody -> the finder (custodian, or the reporter as a fallback).
// Admin custody  -> the Admin Office (holder_* is the admin who received it, if known).
function getItemCustody($pdo, $itemId) {
    $stmt = $pdo->prepare("SELECT fi.id, fi.item_name, fi.status, fi.reported_by, fi.custody_holder, fi.custodian_id, fi.custody_note,
                                  c.first_name AS c_first, c.last_name AS c_last, c.email AS c_email, c.contact_number AS c_phone,
                                  r.first_name AS r_first, r.last_name AS r_last, r.email AS r_email, r.contact_number AS r_phone
                           FROM found_items fi
                           LEFT JOIN users c ON c.id = fi.custodian_id
                           LEFT JOIN users r ON r.id = fi.reported_by
                           WHERE fi.id = :id");
    $stmt->execute(["id" => $itemId]);
    $item = $stmt->fetch(PDO::FETCH_ASSOC);
    if (!$item) return null;

    if ($item['custody_holder'] === 'Finder') {
        $useC = !empty($item['custodian_id']);
        $item['holder_user_id'] = $useC ? $item['custodian_id'] : $item['reported_by'];
        $item['holder_name']    = $useC ? fullName($item['c_first'], $item['c_last']) : fullName($item['r_first'], $item['r_last']);
        $item['holder_email']   = $useC ? $item['c_email'] : $item['r_email'];
        $item['holder_phone']   = $useC ? $item['c_phone'] : $item['r_phone'];
    } else {
        $item['holder_user_id'] = $item['custodian_id'];
        $item['holder_name']    = fullName($item['c_first'], $item['c_last']);
        $item['holder_email']   = $item['c_email'];
        $item['holder_phone']   = $item['c_phone'];
    }
    return $item;
}

// Human-readable "who has it", e.g. for pickup instructions.
// Contact details are only included when $includeContact is true (after a claim is approved).
function custodyDescription($item, $includeContact) {
    if (!$item) return 'the item holder';

    if ($item['custody_holder'] === 'Admin') {
        $text = 'the Admin Office';
        if ($item['holder_name'] !== '') $text .= ' (received by ' . $item['holder_name'] . ')';
    } else {
        $text = ($item['holder_name'] !== '' ? $item['holder_name'] : 'the finder') . ' (the finder)';
        if ($includeContact) {
            $bits = array_filter([$item['holder_email'], $item['holder_phone']]);
            if ($bits) $text .= ' - ' . implode(', ', $bits);
        }
    }
    if (!empty($item['custody_note'])) $text .= ' (note: ' . $item['custody_note'] . ')';
    return $text;
}