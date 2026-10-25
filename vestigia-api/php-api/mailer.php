<?php
// ============================================================
// mailer.php — email sending for Vestigia
// Settings come from mail_config.php.
// ============================================================

use PHPMailer\PHPMailer\PHPMailer;

function mailConfig() {
    static $config = null;
    if ($config === null) {
        $config = require __DIR__ . '/mail_config.php';
    }
    return $config;
}

// Loads PHPMailer from php-api/lib/PHPMailer/ (or Composer's vendor folder if you use it)
function loadPhpMailer() {
    if (class_exists('PHPMailer\PHPMailer\PHPMailer', false)) return true;

    $dir = __DIR__ . '/lib/PHPMailer';
    foreach (['Exception.php', 'PHPMailer.php', 'SMTP.php'] as $file) {
        if (is_readable("$dir/$file")) require_once "$dir/$file";
    }

    if (!class_exists('PHPMailer\PHPMailer\PHPMailer')) {
        $autoload = __DIR__ . '/../vendor/autoload.php';
        if (is_readable($autoload)) require_once $autoload;
    }
    return class_exists('PHPMailer\PHPMailer\PHPMailer');
}

function recordEmail($pdo, $to, $subject, $body, $status, $error = null) {
    try {
        $stmt = $pdo->prepare("INSERT INTO email_outbox (to_email, subject, body, status, error)
                               VALUES (:to, :subject, :body, :status, :error)");
        $stmt->execute([
            "to" => $to, "subject" => $subject, "body" => $body,
            "status" => $status, "error" => $error
        ]);
    } catch (Throwable $e) {
        error_log("Vestigia: could not write email_outbox: " . $e->getMessage());
    }
}

// Never throws. Returns true when the email was sent (smtp) or recorded (log).
function sendEmail($pdo, $toEmail, $subject, $htmlBody, $textBody) {
    $cfg  = mailConfig();
    $mode = strtolower($cfg['mode'] ?? 'log');

    if ($mode !== 'smtp') {
        recordEmail($pdo, $toEmail, $subject, $textBody, 'logged'); // full body kept so you can read the code
        return true;
    }

    if (!loadPhpMailer()) {
        recordEmail($pdo, $toEmail, $subject, '(body not stored)', 'failed', 'PHPMailer is not installed.');
        return false;
    }

    try {
        $mail = new PHPMailer(true);
        $mail->isSMTP();
        $mail->Host       = $cfg['host'] ?? 'smtp.gmail.com';
        $mail->SMTPAuth   = true;
        $mail->Username   = $cfg['username'] ?? '';
        $mail->Password   = $cfg['password'] ?? '';
        $enc              = strtolower($cfg['encryption'] ?? 'tls');
        $mail->SMTPSecure = ($enc === 'ssl') ? PHPMailer::ENCRYPTION_SMTPS : PHPMailer::ENCRYPTION_STARTTLS;
        $mail->Port       = (int) ($cfg['port'] ?? ($enc === 'ssl' ? 465 : 587));
        $mail->CharSet    = 'UTF-8';
        $mail->Timeout    = 12;

        if (($cfg['verify_peer'] ?? true) === false) {
            $mail->SMTPOptions = ['ssl' => [
                'verify_peer' => false, 'verify_peer_name' => false, 'allow_self_signed' => true
            ]];
        }

        $mail->setFrom($cfg['from_address'] ?? $mail->Username, $cfg['from_name'] ?? 'Vestigia');
        $mail->addAddress($toEmail);
        $mail->isHTML(true);
        $mail->Subject = $subject;
        $mail->Body    = $htmlBody;
        $mail->AltBody = $textBody;
        $mail->send();

        recordEmail($pdo, $toEmail, $subject, '(body not stored)', 'sent');
        return true;
    } catch (Throwable $e) {
        error_log("Vestigia: email to $toEmail failed: " . $e->getMessage());
        recordEmail($pdo, $toEmail, $subject, '(body not stored)', 'failed', substr($e->getMessage(), 0, 500));
        return false;
    }
}