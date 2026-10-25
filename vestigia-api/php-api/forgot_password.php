<?php
require_once "db.php";
require_once "password_reset_helper.php";

$data = json_decode(file_get_contents("php://input"), true) ?? [];
$identifier = trim($data['identifier'] ?? '');

if ($identifier === '') {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Enter your email or Student ID."]);
    exit;
}

$pdo = getDbConnection();

// The same answer whether or not an account exists, so this can't be used to look up accounts
$generic = ["success" => true,
            "message" => "If an account matches, a 6-digit reset code has been sent to its email address."];

$user = findUserByIdentifier($pdo, $identifier);
if (!$user) {
    echo json_encode($generic);
    exit;
}

try {
    $result = issuePasswordResetCode($pdo, $user);
} catch (Throwable $e) {
    error_log("Vestigia forgot_password failed: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not send a code right now. Please try again."]);
    exit;
}

if (!$result['ok']) { // asked again within a minute: the earlier code is still valid
    echo json_encode($generic);
    exit;
}

if (!$result['email_sent']) {
    http_response_code(503);
    echo json_encode(["success" => false, "message" => "We couldn't send the email right now. Please try again in a moment."]);
    exit;
}

echo json_encode($generic);