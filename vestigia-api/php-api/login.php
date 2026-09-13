<?php
require_once "db.php";

$data = json_decode(file_get_contents("php://input"), true);

$identifier = trim($data["identifier"] ?? "");   // username/email
$password   = trim($data["password"] ?? "");

if ($identifier === "" || $password === "") {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Email/Student ID and password are required."]);
    exit;
}

$pdo = getDbConnection();

$stmt = $pdo->prepare("SELECT * FROM users WHERE email = :id OR student_id = :id LIMIT 1");
$stmt->execute(["id" => $identifier]);
$user = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$user || !password_verify($password, $user["password_hash"])) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Incorrect email/ID or password."]);
    exit;
}

// create a new session token (valid 7 days)
$token = bin2hex(random_bytes(32));
$expiresAt = date("Y-m-d H:i:s", strtotime("+7 days"));

$insert = $pdo->prepare("INSERT INTO sessions (user_id, token, expires_at) VALUES (:uid, :token, :expires)");
$insert->execute(["uid" => $user["id"], "token" => $token, "expires" => $expiresAt]);

echo json_encode([
    "success" => true,
    "message" => "Login successful.",
    "token"   => $token,
    "user"    => [
        "id"         => $user["id"],
        "first_name" => $user["first_name"],
        "last_name"  => $user["last_name"],
        "email"      => $user["email"],
        "student_id" => $user["student_id"]
    ]
]);
