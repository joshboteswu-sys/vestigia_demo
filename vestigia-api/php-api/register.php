<?php
require_once "db.php";
require_once "verification_helper.php";
require_once "upload_helper.php";

// Multipart/form-data
$firstName        = trim($_POST['first_name'] ?? '');
$lastName         = trim($_POST['last_name'] ?? '');
$middleInitial    = trim($_POST['middle_initial'] ?? '');
$studentId        = trim($_POST['student_id'] ?? '');
$contactNumber    = trim($_POST['contact_number'] ?? '');
$email            = strtolower(trim($_POST['email'] ?? ''));
$password         = trim($_POST['password'] ?? '');
$preferredContact = trim($_POST['preferred_contact'] ?? '');

$required = [
    'first_name'     => $firstName,
    'last_name'      => $lastName,
    'student_id'     => $studentId,
    'contact_number' => $contactNumber,
    'email'          => $email,
    'password'       => $password,
];
foreach ($required as $field => $value) {
    if ($value === '') {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "Field '$field' is required."]);
        exit;
    }
}

if (!preg_match('/^\d{2}-\d{4}-\d{6}$/', $studentId)) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Student ID must follow the format 00-0000-000000."]);
    exit;
}

if (!filter_var($email, FILTER_VALIDATE_EMAIL) || strlen($email) > 50) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Invalid email address (max 50 characters)."]);
    exit;
}

if (!preg_match('/^\+?[0-9]{10,15}$/', $contactNumber)) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Invalid contact number."]);
    exit;
}

if (strlen($password) < 6) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Password must be at least 6 characters."]);
    exit;
}

if ($preferredContact !== '' && !in_array($preferredContact, ['Email', 'SMS', 'Push'])) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Invalid preferred contact method."]);
    exit;
}

$pdo = getDbConnection();

$check = $pdo->prepare("SELECT id, email, email_verified FROM users WHERE LOWER(email) = :email OR student_id = :sid");
$check->execute(["email" => $email, "sid" => $studentId]);
$existing = $check->fetch(PDO::FETCH_ASSOC);
if ($existing) {
    $sameEmail = strtolower($existing['email']) === $email;
    $message = ($sameEmail && !dbBool($existing['email_verified']))
        ? "This email is already registered but not verified yet. Sign in to get a new verification code."
        : "An account with this email or student ID already exists.";
    http_response_code(409);
    echo json_encode(["success" => false, "message" => $message]);
    exit;
}

$photoPath = handleImageUpload('photo', 'user_');

$passwordHash = password_hash($password, PASSWORD_BCRYPT);

try {
    $stmt = $pdo->prepare("INSERT INTO users (first_name, last_name, middle_initial, student_id, contact_number, email, password_hash, photo_path, preferred_contact, email_verified)
                           VALUES (:first_name, :last_name, :mi, :sid, :contact, :email, :hash, :photo, :preferred, FALSE) RETURNING id");
    $stmt->execute([
        "first_name" => $firstName,
        "last_name"  => $lastName,
        "mi"         => $middleInitial !== '' ? $middleInitial : null,
        "sid"        => $studentId,
        "contact"    => $contactNumber,
        "email"      => $email,
        "hash"       => $passwordHash,
        "photo"      => $photoPath,
        "preferred"  => $preferredContact !== '' ? $preferredContact : null
    ]);
    $newId = $stmt->fetchColumn();
} catch (PDOException $e) {
    error_log("Vestigia register failed: " . $e->getMessage());
    if ($e->getCode() === '23505') { // unique violation: someone registered the same email/ID a moment ago
        http_response_code(409);
        echo json_encode(["success" => false, "message" => "An account with this email or student ID already exists."]);
    } else {
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Could not create your account right now. Please try again."]);
    }
    exit;
}

// Send the verification code. A mail problem must not undo a successful registration.
$emailSent = false;
try {
    $result = issueVerificationCode($pdo, ["id" => $newId, "first_name" => $firstName, "email" => $email]);
    $emailSent = $result["email_sent"];
} catch (Throwable $e) {
    error_log("Vestigia: could not issue verification code: " . $e->getMessage());
}

echo json_encode([
    "success"            => true,
    "message"            => "Account created. Enter the code we sent to your email.",
    "user_id"            => $newId,
    "needs_verification" => true,
    "email"              => $email,
    "email_sent"         => $emailSent
]);