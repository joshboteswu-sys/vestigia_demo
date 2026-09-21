<?php
// Expects $pageTitle to be set by the including page.
$currentFile = basename($_SERVER['SCRIPT_NAME']);
function navClass($file, $current) {
    return $file === $current ? 'nav-link active' : 'nav-link';
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title><?= htmlspecialchars($pageTitle ?? 'Vestigia Admin') ?></title>
<link rel="stylesheet" href="assets/style.css">
</head>
<body>
<div class="app-shell">
  <aside class="sidebar">
    <div class="brand">Vestigia<span>Admin</span></div>
    <nav>
      <a class="<?= navClass('index.php', $currentFile) ?>" href="index.php">Dashboard</a>
      <a class="<?= navClass('found_items.php', $currentFile) ?>" href="found_items.php">Found Items</a>
      <a class="<?= navClass('lost_items.php', $currentFile) ?>" href="lost_items.php">Lost Items</a>
      <a class="<?= navClass('claims.php', $currentFile) ?>" href="claims.php">Claims</a>
      <a class="<?= navClass('users.php', $currentFile) ?>" href="users.php">Users</a>
    </nav>
    <a class="logout-link" href="logout.php">Log out</a>
  </aside>
  <main class="main-content">
    <header class="topbar">
      <h1><?= htmlspecialchars($pageTitle ?? '') ?></h1>
      <span class="admin-name">Signed in as <?= htmlspecialchars($_SESSION['admin_name'] ?? 'Admin') ?></span>
    </header>
    <div class="content-area">
      <?php if (!empty($_SESSION['flash'])): ?>
        <div class="flash <?= htmlspecialchars($_SESSION['flash']['type']) ?>">
          <?= htmlspecialchars($_SESSION['flash']['message']) ?>
        </div>
        <?php unset($_SESSION['flash']); ?>
      <?php endif; ?>
