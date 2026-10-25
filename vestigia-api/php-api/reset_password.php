<?php
require_once "db.php";
require_once "password_reset_helper.php";

$data = json_decode(file_get_contents("php://input"), true) ?? [];
$identifier  = trim($data['identifier'] ?? '');
$code        = trim($data['code'] ?? '');
$newPassword = trim($data['new_password'] ?? '');

if ($identifier === '' || !preg_match('/^\d{6}$/', $code)) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Enter the 6-digit code from your email."]);
    exit;
}
if (strlen($newPassword) < 6 || strlen($newPassword) > 100) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Your new password must be 6 to 100 characters."]);
    exit;
}

$pdo = getDbConnection();
$invalid = ["success" => false, "message" => "That code is invalid or has expired. Please request a new one."];

$user = findUserByIdentifier($pdo, $identifier);
if (!$user) {
    http_response_code(400);
    echo json_encode($invalid);
    exit;
}

$stmt = $pdo->prepare("SELECT id, code_hash, attempts, expires_at FROM password_resets WHERE user_id = :uid");
$stmt->execute(["uid" => $user['id']]);
$row = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$row) {
    http_response_code(400);
    echo json_encode($invalid);
    exit;
}

$deleteRow = $pdo->prepare("DELETE FROM password_resets WHERE id = :id");

if (strtotime($row['expires_at']) < time()) {
    $deleteRow->execute(["id" => $row['id']]);
    http_response_code(400);
    echo json_encode($invalid);
    exit;
}

if ((int) $row['attempts'] >= RESET_MAX_ATTEMPTS) {
    $deleteRow->execute(["id" => $row['id']]);
    http_response_code(429);
    echo json_encode(["success" => false, "message" => "Too many incorrect attempts. Please request a new code."]);
    exit;
}

if (!password_verify($code, $row['code_hash'])) {
    $attempts = (int) $row['attempts'] + 1;
    if ($attempts >= RESET_MAX_ATTEMPTS) {
        $deleteRow->execute(["id" => $row['id']]);
        http_response_code(429);
        echo json_encode(["success" => false, "message" => "Too many incorrect attempts. Please request a new code."]);
        exit;
    }
    $pdo->prepare("UPDATE password_resets SET attempts = :a WHERE id = :id")->execute(["a" => $attempts, "id" => $row['id']]);
    $left = RESET_MAX_ATTEMPTS - $attempts;
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Incorrect code. $left attempt" . ($left === 1 ? "" : "s") . " left."]);
    exit;
}

// Correct code: set the new password and sign the user out everywhere
$pdo->beginTransaction();
try {
    $pdo->prepare("UPDATE users SET password_hash = :hash, email_verified = TRUE,
                          email_verified_at = COALESCE(email_verified_at, CURRENT_TIMESTAMP)
                    WHERE id = :id")
        ->execute(["hash" => password_hash($newPassword, PASSWORD_BCRYPT), "id" => $user['id']]);
    $pdo->prepare("DELETE FROM password_resets WHERE user_id = :uid")->execute(["uid" => $user['id']]);
    $pdo->prepare("DELETE FROM sessions WHERE user_id = :uid")->execute(["uid" => $user['id']]);
    $pdo->commit();
} catch (Throwable $e) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    error_log("Vestigia reset_password failed: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not reset your password. Please try again."]);
    exit;
}

// Tell the owner, in case it wasn't them
$name = htmlspecialchars($user['first_name'], ENT_QUOTES, 'UTF-8');
sendEmail($pdo, $user['email'], "Your Vestigia password was changed",
    "<p>Hi $name,</p><p>Your Vestigia password was just changed. If this wasn't you, please contact an administrator right away.</p>",
    "Hi {$user['first_name']},\n\nYour Vestigia password was just changed. If this wasn't you, please contact an administrator right away.\n\n- Vestigia");

echo json_encode(["success" => true, "message" => "Password updated. You can sign in with your new password."]);