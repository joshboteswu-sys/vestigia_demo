<?php
require_once __DIR__ . '/../includes/auth.php';
require_once __DIR__ . '/../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    header('Location: ../found_items.php');
    exit;
}

$id = filter_input(INPUT_POST, 'id', FILTER_VALIDATE_INT);
if (!$id) {
    $_SESSION['flash'] = ['type' => 'error', 'message' => 'Invalid item.'];
    header('Location: ../found_items.php');
    exit;
}

if (!empty($_POST['delete'])) {
    $stmt = $pdo->prepare('DELETE FROM found_items WHERE id = ?');
    $stmt->execute([$id]);
    $_SESSION['flash'] = ['type' => 'success', 'message' => 'Item deleted.'];
} else {
    $status = $_POST['status'] ?? '';
    if (!in_array($status, ['Unclaimed', 'Claimed'], true)) {
        $_SESSION['flash'] = ['type' => 'error', 'message' => 'Invalid status.'];
        header('Location: ../found_items.php');
        exit;
    }
    $stmt = $pdo->prepare('UPDATE found_items SET status = ? WHERE id = ?');
    $stmt->execute([$status, $id]);
    $_SESSION['flash'] = ['type' => 'success', 'message' => 'Item status updated to ' . $status . '.'];
}

header('Location: ../found_items.php');
exit;
