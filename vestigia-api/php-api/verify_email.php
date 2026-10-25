<?php
require_once "db.php";
require_once "verification_helper.php";

$data  = json_decode(file_get_contents("php://input"), true) ?? [];
$email = trim($data['email'] ?? '');
$code  = trim($data['code'] ?? '');

if ($email === '' || !preg_match('/^\d{6}$/', $code)) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Enter the 6-digit code from your email."]);
    exit;
}

$pdo = getDbConnection();

$stmt = $pdo->prepare("SELECT id, email_verified FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1");
$stmt->execute(["email" => $email]);
$user = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$user) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "That code is invalid or has expired."]);
    exit;
}

if (dbBool($user['email_verified'])) {
    echo json_encode(["success" => true, "message" => "Your email is already verified."]);
    exit;
}

$stmt = $pdo->prepare("SELECT id, code_hash, attempts, expires_at FROM email_verifications WHERE user_id = :uid");
$stmt->execute(["uid" => $user['id']]);
$row = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$row) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "There is no active code. Please request a new one."]);
    exit;
}

$deleteRow = $pdo->prepare("DELETE FROM email_verifications WHERE id = :id");

if (strtotime($row['expires_at']) < time()) {
    $deleteRow->execute(["id" => $row['id']]);
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "That code has expired. Please request a new one."]);
    exit;
}

if ((int) $row['attempts'] >= VERIFY_MAX_ATTEMPTS) {
    $deleteRow->execute(["id" => $row['id']]);
    http_response_code(429);
    echo json_encode(["success" => false, "message" => "Too many incorrect attempts. Please request a new code."]);
    exit;
}

if (!password_verify($code, $row['code_hash'])) {
    $attempts = (int) $row['attempts'] + 1;

    if ($attempts >= VERIFY_MAX_ATTEMPTS) {
        $deleteRow->execute(["id" => $row['id']]);
        http_response_code(429);
        echo json_encode(["success" => false, "message" => "Too many incorrect attempts. Please request a new code."]);
        exit;
    }

    $pdo->prepare("UPDATE email_verifications SET attempts = :a WHERE id = :id")
        ->execute(["a" => $attempts, "id" => $row['id']]);

    $left = VERIFY_MAX_ATTEMPTS - $attempts;
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Incorrect code. $left attempt" . ($left === 1 ? "" : "s") . " left."]);
    exit;
}

// Correct code
$pdo->beginTransaction();
try {
    $pdo->prepare("UPDATE users SET email_verified = TRUE, email_verified_at = CURRENT_TIMESTAMP WHERE id = :id")
        ->execute(["id" => $user['id']]);
    $pdo->prepare("DELETE FROM email_verifications WHERE user_id = :uid")
        ->execute(["uid" => $user['id']]);
    $pdo->commit();
} catch (Throwable $e) {
    $pdo->rollBack();
    error_log("Vestigia: verify_email failed: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not verify your email. Please try again."]);
    exit;
}

echo json_encode(["success" => true, "message" => "Email verified. You can sign in now."]);