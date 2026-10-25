<?php
require_once "verification_helper.php"; // brings in mailer.php (sendEmail) and dbBool()

const RESET_MAX_ATTEMPTS    = 5;
const RESET_RESEND_COOLDOWN = 60; // seconds

// A Student ID typed with or without dashes becomes 00-0000-000000; anything else is left alone
function normalizeIdentifier($identifier) {
    $identifier = trim($identifier);
    $digits = preg_replace('/\D/', '', $identifier);
    if (strlen($digits) === 12 && preg_match('/^[\d-]+$/', $identifier)) {
        return substr($digits, 0, 2) . '-' . substr($digits, 2, 4) . '-' . substr($digits, 6);
    }
    return $identifier;
}

function findUserByIdentifier($pdo, $identifier) {
    $stmt = $pdo->prepare("SELECT id, first_name, email FROM users
                           WHERE LOWER(email) = LOWER(:id) OR student_id = :id LIMIT 1");
    $stmt->execute(["id" => normalizeIdentifier($identifier)]);
    return $stmt->fetch(PDO::FETCH_ASSOC) ?: null;
}

function passwordResetEmailContent($firstName, $code, $minutes) {
    $text = "Hi $firstName,\n\n"
          . "Your Vestigia password reset code is: $code\n\n"
          . "It expires in $minutes minutes. If you didn't ask to reset your password, you can ignore this email "
          . "and your password will stay the same.\n\n- Vestigia";

    $name = htmlspecialchars($firstName, ENT_QUOTES, 'UTF-8');
    $html = '<div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:24px;color:#0F172A">'
          . '<h2 style="color:#0F766E;margin:0 0 4px">VESTIGIA</h2>'
          . '<p style="color:#64748B;margin:0 0 24px">Tracing what\'s missing</p>'
          . "<p>Hi $name,</p><p>Use this code to reset your password:</p>"
          . '<p style="font-size:34px;letter-spacing:8px;font-weight:bold;color:#0F766E;background:#E6F7F4;'
          . 'padding:16px;text-align:center;border-radius:12px">' . $code . '</p>'
          . "<p style=\"color:#64748B\">This code expires in $minutes minutes. If you didn't ask to reset your "
          . "password, you can ignore this email and your password will stay the same.</p></div>";

    return [$html, $text];
}

// Returns ["ok" => bool, "retry_after" => int, "email_sent" => bool]
function issuePasswordResetCode($pdo, $user) {
    $stmt = $pdo->prepare("SELECT created_at FROM password_resets WHERE user_id = :uid");
    $stmt->execute(["uid" => $user['id']]);
    $existing = $stmt->fetchColumn();
    if ($existing) {
        $elapsed = time() - strtotime($existing);
        if ($elapsed < RESET_RESEND_COOLDOWN) {
            return ["ok" => false, "retry_after" => RESET_RESEND_COOLDOWN - $elapsed, "email_sent" => false];
        }
    }

    $code    = str_pad((string) random_int(0, 999999), 6, '0', STR_PAD_LEFT);
    $minutes = verificationMinutes();
    $now     = date("Y-m-d H:i:s");
    $expires = date("Y-m-d H:i:s", time() + $minutes * 60);

    $pdo->beginTransaction();
    try {
        $pdo->prepare("DELETE FROM password_resets WHERE user_id = :uid")->execute(["uid" => $user['id']]);
        $pdo->prepare("INSERT INTO password_resets (user_id, code_hash, attempts, expires_at, created_at)
                       VALUES (:uid, :hash, 0, :exp, :now)")
            ->execute(["uid" => $user['id'], "hash" => password_hash($code, PASSWORD_DEFAULT),
                       "exp" => $expires, "now" => $now]);
        $pdo->commit();
    } catch (Throwable $e) {
        $pdo->rollBack();
        throw $e;
    }

    list($html, $text) = passwordResetEmailContent($user['first_name'], $code, $minutes);
    $sent = sendEmail($pdo, $user['email'], "Your Vestigia password reset code", $html, $text);

    if (!$sent) { // no email went out, so don't leave a code the user can't know
        $pdo->prepare("DELETE FROM password_resets WHERE user_id = :uid")->execute(["uid" => $user['id']]);
    }
    return ["ok" => true, "retry_after" => 0, "email_sent" => $sent];
}