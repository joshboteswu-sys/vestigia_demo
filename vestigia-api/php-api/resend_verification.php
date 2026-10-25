<?php
require_once "db.php";
require_once "verification_helper.php";

$data  = json_decode(file_get_contents("php://input"), true) ?? [];
$email = trim($data['email'] ?? '');

if ($email === '') {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Email is required."]);
    exit;
}

$pdo = getDbConnection();

$stmt = $pdo->prepare("SELECT id, first_name, email, email_verified FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1");
$stmt->execute(["email" => $email]);
$user = $stmt->fetch(PDO::FETCH_ASSOC);

$generic = ["success" => true, "message" => "If this email belongs to an unverified account, a new code has been sent."];

// Same answer whether or not the account exists, so this can't be used to probe for emails
if (!$user || dbBool($user['email_verified'])) {
    echo json_encode($generic);
    exit;
}

try {
    $result = issueVerificationCode($pdo, $user);
} catch (Throwable $e) {
    error_log("Vestigia: resend_verification failed: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not send a code right now. Please try again."]);
    exit;
}

if (!$result['ok']) {
    http_response_code(429);
    echo json_encode([
        "success" => false,
        "message" => "Please wait {$result['retry_after']} seconds before requesting another code.",
        "retry_after" => $result['retry_after']
    ]);
    exit;
}

if (!$result['email_sent']) {
    http_response_code(503);
    echo json_encode(["success" => false, "message" => "We couldn't send the email right now. Please try again in a moment."]);
    exit;
}

echo json_encode($generic);