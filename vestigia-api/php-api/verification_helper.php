<?php
require_once "mailer.php";

const VERIFY_MAX_ATTEMPTS    = 5;
const VERIFY_RESEND_COOLDOWN = 60; // seconds

// PDO/Postgres can return booleans as true/false or 't'/'f'
function dbBool($v) {
    return $v === true || $v === 't' || $v === 'true' || $v === 1 || $v === '1';
}

function verificationMinutes() {
    return max(1, (int) (mailConfig()['code_minutes'] ?? 10));
}

function verificationEmailContent($firstName, $code, $minutes) {
    $text = "Hi $firstName,\n\n"
          . "Your Vestigia verification code is: $code\n\n"
          . "It expires in $minutes minutes. If you didn't create a Vestigia account, you can ignore this email.\n\n"
          . "- Vestigia";

    $name = htmlspecialchars($firstName, ENT_QUOTES, 'UTF-8');
    $html = '<div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:24px;color:#0F172A">'
          . '<h2 style="color:#0F766E;margin:0 0 4px">VESTIGIA</h2>'
          . '<p style="color:#64748B;margin:0 0 24px">Tracing what\'s missing</p>'
          . "<p>Hi $name,</p>"
          . '<p>Use this code to verify your email address:</p>'
          . '<p style="font-size:34px;letter-spacing:8px;font-weight:bold;color:#0F766E;background:#E6F7F4;'
          . 'padding:16px;text-align:center;border-radius:12px">' . $code . '</p>'
          . "<p style=\"color:#64748B\">This code expires in $minutes minutes. "
          . "If you didn't create a Vestigia account, you can ignore this email.</p>"
          . '</div>';

    return [$html, $text];
}

// Creates a fresh code for the user, stores its hash, and emails it.
// $user needs: id, first_name, email.
// Returns ["ok" => bool, "retry_after" => int, "email_sent" => bool]
function issueVerificationCode($pdo, $user) {
    // Resend cooldown
    $stmt = $pdo->prepare("SELECT created_at FROM email_verifications WHERE user_id = :uid");
    $stmt->execute(["uid" => $user['id']]);
    $existing = $stmt->fetchColumn();
    if ($existing) {
        $elapsed = time() - strtotime($existing);
        if ($elapsed < VERIFY_RESEND_COOLDOWN) {
            return ["ok" => false, "retry_after" => VERIFY_RESEND_COOLDOWN - $elapsed, "email_sent" => false];
        }
    }

    $code    = str_pad((string) random_int(0, 999999), 6, '0', STR_PAD_LEFT);
    $minutes = verificationMinutes();
    $now     = date("Y-m-d H:i:s");
    $expires = date("Y-m-d H:i:s", time() + $minutes * 60);

    $pdo->beginTransaction();
    try {
        $pdo->prepare("DELETE FROM email_verifications WHERE user_id = :uid")
            ->execute(["uid" => $user['id']]);
        $pdo->prepare("INSERT INTO email_verifications (user_id, code_hash, attempts, expires_at, created_at)
                       VALUES (:uid, :hash, 0, :exp, :now)")
            ->execute([
                "uid" => $user['id'], "hash" => password_hash($code, PASSWORD_DEFAULT),
                "exp" => $expires, "now" => $now
            ]);
        $pdo->commit();
    } catch (Throwable $e) {
        $pdo->rollBack();
        throw $e;
    }

    list($html, $text) = verificationEmailContent($user['first_name'], $code, $minutes);
    $sent = sendEmail($pdo, $user['email'], "Your Vestigia verification code", $html, $text);

    // If the email could not be sent, drop the code so the user can retry immediately
    if (!$sent) {
        $pdo->prepare("DELETE FROM email_verifications WHERE user_id = :uid")->execute(["uid" => $user['id']]);
    }

    return ["ok" => true, "retry_after" => 0, "email_sent" => $sent];
}