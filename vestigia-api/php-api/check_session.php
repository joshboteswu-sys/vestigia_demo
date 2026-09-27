<?php
require_once "db.php";
require_once "auth_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo); // exits with 401 automatically if invalid/expired

echo json_encode([
    "success" => true,
    "user" => [
        "id"         => $session["user_id"],
        "first_name" => $session["first_name"],
        "last_name"  => $session["last_name"],
        "email"      => $session["email"]
    ]
]);
