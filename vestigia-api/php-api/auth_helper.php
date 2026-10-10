<?php
function getBearerToken() {
    $headers = null;
    if (function_exists('getallheaders')) {
        $headers = getallheaders();
    }
    $authHeader = null;
    if ($headers) {
        foreach ($headers as $key => $value) {
            if (strtolower($key) === 'authorization') {
                $authHeader = $value;
            }
        }
    }
    if (!$authHeader && isset($_SERVER['HTTP_AUTHORIZATION'])) {
        $authHeader = $_SERVER['HTTP_AUTHORIZATION'];
    }
    if ($authHeader && preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) {
        return $matches[1];
    }
    return null;
}

function requireAuth($pdo) {
    $token = getBearerToken();
    if (!$token) {
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "Missing session token. Please log in again."]);
        exit;
    }

    $stmt = $pdo->prepare("SELECT s.user_id, s.expires_at, u.first_name, u.last_name, u.email, u.is_admin
                            FROM sessions s
                            JOIN users u ON u.id = s.user_id
                            WHERE s.token = :token");
    $stmt->execute(["token" => $token]);
    $session = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$session) {
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "Invalid session. Please log in again."]);
        exit;
    }

    if (strtotime($session['expires_at']) < time()) {
        $del = $pdo->prepare("DELETE FROM sessions WHERE token = :token");
        $del->execute(["token" => $token]);
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "Session expired. Please log in again."]);
        exit;
    }

    // Normalize Postgres boolean (can come back as true/false or 't'/'f' depending on driver)
    $session['is_admin'] = ($session['is_admin'] === true || $session['is_admin'] === 't' || $session['is_admin'] === '1');

    return $session; // contains user_id, first_name, last_name, email, is_admin
}

// Shared ownership check used by every update/delete endpoint (found or lost items).
// Exits with 403 if the current user is neither the original reporter nor an admin.
function requireOwnerOrAdmin($session, $reportedBy) {
    $isOwner = ((int)$reportedBy === (int)$session['user_id']);
    if (!$isOwner && !$session['is_admin']) {
        http_response_code(403);
        echo json_encode(["success" => false, "message" => "You can only edit or delete items you reported."]);
        exit;
    }
}