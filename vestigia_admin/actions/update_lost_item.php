<?php
require_once __DIR__ . '/../includes/auth.php';
require_once __DIR__ . '/../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    header('Location: ../lost_items.php');
    exit;
}

$id = filter_input(INPUT_POST, 'id', FILTER_VALIDATE_INT);
if (!$id) {
    $_SESSION['flash'] = ['type' => 'error', 'message' => 'Invalid item.'];
    header('Location: ../lost_items.php');
    exit;
}

if (!empty($_POST['delete'])) {
    $stmt = $pdo->prepare('DELETE FROM lost_items WHERE id = ?');
    $stmt->execute([$id]);
    $_SESSION['flash'] = ['type' => 'success', 'message' => 'Report deleted.'];
} else {
    $status = $_POST['status'] ?? '';
    if (!in_array($status, ['Missing', 'Found', 'Closed'], true)) {
        $_SESSION['flash'] = ['type' => 'error', 'message' => 'Invalid status.'];
        header('Location: ../lost_items.php');
        exit;
    }
    $stmt = $pdo->prepare('UPDATE lost_items SET status = ? WHERE id = ?');
    $stmt->execute([$status, $id]);
    $_SESSION['flash'] = ['type' => 'success', 'message' => 'Report status updated to ' . $status . '.'];
}

header('Location: ../lost_items.php');
exit;
