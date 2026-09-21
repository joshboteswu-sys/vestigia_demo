<?php
require_once __DIR__ . '/../includes/auth.php';
require_once __DIR__ . '/../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    header('Location: ../claims.php');
    exit;
}

$id = filter_input(INPUT_POST, 'id', FILTER_VALIDATE_INT);
$action = $_POST['action'] ?? '';

if (!$id || !in_array($action, ['approve', 'reject'], true)) {
    $_SESSION['flash'] = ['type' => 'error', 'message' => 'Invalid request.'];
    header('Location: ../claims.php');
    exit;
}

$stmt = $pdo->prepare('SELECT item_id, status FROM claims WHERE id = ? LIMIT 1');
$stmt->execute([$id]);
$claim = $stmt->fetch();

if (!$claim) {
    $_SESSION['flash'] = ['type' => 'error', 'message' => 'Claim not found.'];
    header('Location: ../claims.php');
    exit;
}

if ($claim['status'] !== 'Pending') {
    $_SESSION['flash'] = ['type' => 'error', 'message' => 'This claim has already been reviewed.'];
    header('Location: ../claims.php');
    exit;
}

$pdo->beginTransaction();
try {
    if ($action === 'approve') {
        $pdo->prepare("UPDATE claims SET status = 'Approved' WHERE id = ?")->execute([$id]);
        $pdo->prepare("UPDATE found_items SET status = 'Claimed' WHERE id = ?")->execute([$claim['item_id']]);
        // Any other still-pending claims on the same item are auto-rejected,
        // since the item can only go to one claimant.
        $pdo->prepare("UPDATE claims SET status = 'Rejected' WHERE item_id = ? AND id != ? AND status = 'Pending'")
            ->execute([$claim['item_id'], $id]);
        $message = 'Claim approved. Item marked as Claimed.';
    } else {
        $pdo->prepare("UPDATE claims SET status = 'Rejected' WHERE id = ?")->execute([$id]);
        $message = 'Claim rejected.';
    }
    $pdo->commit();
    $_SESSION['flash'] = ['type' => 'success', 'message' => $message];
} catch (Exception $e) {
    $pdo->rollBack();
    $_SESSION['flash'] = ['type' => 'error', 'message' => 'Could not update the claim. Please try again.'];
}

header('Location: ../claims.php');
exit;
