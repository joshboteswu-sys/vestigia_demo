<?php
require_once "db.php";

$data = json_decode(file_get_contents("php://input"), true);

$required = ["first_name", "last_name", "student_id", "email", "password"];
foreach ($required as $field) {
    if (empty($data[$field])) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "Field '$field' is required."]);
        exit;
    }
}

if (!filter_var($data["email"], FILTER_VALIDATE_EMAIL)) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Invalid email address."]);
    exit;
}

if (strlen($data["password"]) < 6) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Password must be at least 6 characters."]);
    exit;
}

$pdo = getDbConnection();

// check duplicates
$check = $pdo->prepare("SELECT id FROM users WHERE email = :email OR student_id = :sid");
$check->execute(["email" => $data["email"], "sid" => $data["student_id"]]);
if ($check->fetch()) {
    http_response_code(409);
    echo json_encode(["success" => false, "message" => "An account with this email or student ID already exists."]);
    exit;
}

$passwordHash = password_hash($data["password"], PASSWORD_BCRYPT); // never store plain text

$stmt = $pdo->prepare("INSERT INTO users (first_name, last_name, middle_initial, student_id, contact_number, email, password_hash)
                       VALUES (:first_name, :last_name, :mi, :sid, :contact, :email, :hash) RETURNING id");
$stmt->execute([
    "first_name" => $data["first_name"],
    "last_name"  => $data["last_name"],
    "mi"         => $data["middle_initial"] ?? null,
    "sid"        => $data["student_id"],
    "contact"    => $data["contact_number"] ?? null,
    "email"      => $data["email"],
    "hash"       => $passwordHash
]);
$newId = $stmt->fetchColumn();

echo json_encode(["success" => true, "message" => "Account created successfully.", "user_id" => $newId]);
