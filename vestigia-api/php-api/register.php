<?php
require_once "db.php";

// Multipart/form-data
$firstName        = trim($_POST['first_name'] ?? '');
$lastName         = trim($_POST['last_name'] ?? '');
$middleInitial    = trim($_POST['middle_initial'] ?? '');
$studentId        = trim($_POST['student_id'] ?? '');
$contactNumber    = trim($_POST['contact_number'] ?? '');
$email            = trim($_POST['email'] ?? '');
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

// Student ID must be 00-0000-000000
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

$check = $pdo->prepare("SELECT id FROM users WHERE email = :email OR student_id = :sid");
$check->execute(["email" => $email, "sid" => $studentId]);
if ($check->fetch()) {
    http_response_code(409);
    echo json_encode(["success" => false, "message" => "An account with this email or student ID already exists."]);
    exit;
}

$photoPath = null;
if (isset($_FILES['photo']) && $_FILES['photo']['error'] === UPLOAD_ERR_OK) {
    $uploadsDir = __DIR__ . '/uploads';
    if (!is_dir($uploadsDir)) {
        mkdir($uploadsDir, 0777, true);
    }
    $ext = strtolower(pathinfo($_FILES['photo']['name'], PATHINFO_EXTENSION));
    $allowed = ['jpg', 'jpeg', 'png', 'webp'];
    if (in_array($ext, $allowed)) {
        $filename = uniqid('user_', true) . '.' . $ext;
        if (move_uploaded_file($_FILES['photo']['tmp_name'], $uploadsDir . '/' . $filename)) {
            $photoPath = 'uploads/' . $filename;
        }
    }
}

$passwordHash = password_hash($password, PASSWORD_BCRYPT);

try {
    $stmt = $pdo->prepare("INSERT INTO users (first_name, last_name, middle_initial, student_id, contact_number, email, password_hash, photo_path, preferred_contact)
                           VALUES (:first_name, :last_name, :mi, :sid, :contact, :email, :hash, :photo, :preferred) RETURNING id");
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

    echo json_encode(["success" => true, "message" => "Account created successfully.", "user_id" => $newId]);

} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not create account: " . $e->getMessage()]);
}