<?php
session_start();
require_once __DIR__ . '/config/db.php';

if (!empty($_SESSION['admin_id'])) {
    header('Location: index.php');
    exit;
}

$error = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $email = trim($_POST['email'] ?? '');
    $password = $_POST['password'] ?? '';

    if ($email === '' || $password === '') {
        $error = 'Please enter both email and password.';
    } else {
        $stmt = $pdo->prepare('SELECT id, first_name, last_name, password_hash, is_admin FROM users WHERE email = ? LIMIT 1');
        $stmt->execute([$email]);
        $user = $stmt->fetch();

        if (!$user || !password_verify($password, $user['password_hash'])) {
            $error = 'Invalid email or password.';
        } elseif (!$user['is_admin']) {
            $error = 'This account does not have admin access.';
        } else {
            $_SESSION['admin_id'] = $user['id'];
            $_SESSION['admin_name'] = $user['first_name'] . ' ' . $user['last_name'];
            header('Location: index.php');
            exit;
        }
    }
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Admin Login &mdash; Vestigia</title>
<link rel="stylesheet" href="assets/style.css">
</head>
<body class="login-body">
  <form class="login-card" method="post" action="login.php">
    <div class="login-brand">Vestigia<span>Admin</span></div>
    <p class="login-sub">Lost &amp; Found Management System</p>

    <?php if ($error): ?>
      <div class="flash error"><?= htmlspecialchars($error) ?></div>
    <?php endif; ?>

    <label for="email">Email</label>
    <input type="email" id="email" name="email" required autofocus value="<?= htmlspecialchars($_POST['email'] ?? '') ?>">

    <label for="password">Password</label>
    <input type="password" id="password" name="password" required>

    <button type="submit">Log In</button>
  </form>
</body>
</html>
