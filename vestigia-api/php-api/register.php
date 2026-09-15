<?php
require_once "db.php";

// Now expects multipart/form-data instead of JSON, so a photo file can be attached.
$firstName = trim($_POST['first_name'] ?? '');
$lastName = trim($_POST['last_name'] ?? '');
$middleInitial = trim($_POST['middle_initial'] ?? '');
$studentId = trim($_POST['student_id'] ?? '');
$contactNumber = trim($_POST['contact_number'] ?? '');
$email = trim($_POST['email'] ?? '');
$password = trim($_POST['password'] ?? '');

$required = compact('firstName', 'lastName', 'studentId', 'email', 'password');
foreach ($required as $key => $value) {
    if ($value === '') {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "Field '$key' is required."]);
        exit;
    }
}

if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Invalid email address."]);
    exit;
}

if (strlen($password) < 6) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Password must be at least 6 characters."]);
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

$passwordHash = password_hash($password, PASSWORD_BCRYPT); // never store plain text

$stmt = $pdo->prepare("INSERT INTO users (first_name, last_name, middle_initial, student_id, contact_number, email, password_hash, photo_path)
                       VALUES (:first_name, :last_name, :mi, :sid, :contact, :email, :hash, :photo) RETURNING id");
$stmt->execute([
    "first_name" => $firstName,
    "last_name"  => $lastName,
    "mi"         => $middleInitial,
    "sid"        => $studentId,
    "contact"    => $contactNumber,
    "email"      => $email,
    "hash"       => $passwordHash,
    "photo"      => $photoPath
]);
$newId = $stmt->fetchColumn();

echo json_encode(["success" => true, "message" => "Account created successfully.", "user_id" => $newId]);