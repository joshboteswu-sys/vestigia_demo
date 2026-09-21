<?php
require_once __DIR__ . '/../includes/auth.php';
require_once __DIR__ . '/../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    header('Location: ../users.php');
    exit;
}

$id = filter_input(INPUT_POST, 'id', FILTER_VALIDATE_INT);
$isAdmin = filter_input(INPUT_POST, 'is_admin', FILTER_VALIDATE_INT);

if (!$id || $isAdmin === null || $isAdmin === false) {
    $_SESSION['flash'] = ['type' => 'error', 'message' => 'Invalid request.'];
    header('Location: ../users.php');
    exit;
}

if ((int)$id === (int)$_SESSION['admin_id']) {
    $_SESSION['flash'] = ['type' => 'error', 'message' => 'You cannot change your own admin access.'];
    header('Location: ../users.php');
    exit;
}

$stmt = $pdo->prepare('UPDATE users SET is_admin = ? WHERE id = ?');
$stmt->execute([$isAdmin ? 1 : 0, $id]);

$_SESSION['flash'] = ['type' => 'success', 'message' => 'User role updated.'];
header('Location: ../users.php');
exit;
