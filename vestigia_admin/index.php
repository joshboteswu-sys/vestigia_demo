<?php
require_once __DIR__ . '/includes/auth.php';
require_once __DIR__ . '/config/db.php';

$pageTitle = 'Dashboard';

$counts = [];
$counts['users']            = $pdo->query('SELECT COUNT(*) FROM users')->fetchColumn();
$counts['found_unclaimed']  = $pdo->query("SELECT COUNT(*) FROM found_items WHERE status = 'Unclaimed'")->fetchColumn();
$counts['found_claimed']    = $pdo->query("SELECT COUNT(*) FROM found_items WHERE status = 'Claimed'")->fetchColumn();
$counts['lost_missing']     = $pdo->query("SELECT COUNT(*) FROM lost_items WHERE status = 'Missing'")->fetchColumn();
$counts['claims_pending']   = $pdo->query("SELECT COUNT(*) FROM claims WHERE status = 'Pending'")->fetchColumn();
$counts['claims_approved']  = $pdo->query("SELECT COUNT(*) FROM claims WHERE status = 'Approved'")->fetchColumn();

$recentClaims = $pdo->query("
    SELECT c.id, c.status, c.claimed_at, f.item_name, u.first_name, u.last_name
    FROM claims c
    JOIN found_items f ON f.id = c.item_id
    JOIN users u ON u.id = c.claimed_by
    ORDER BY c.claimed_at DESC
    LIMIT 5
")->fetchAll();

require_once __DIR__ . '/includes/header.php';
?>
<section class="cards-grid">
  <div class="card">
    <span class="card-label">Registered Users</span>
    <span class="card-value"><?= (int)$counts['users'] ?></span>
  </div>
  <div class="card">
    <span class="card-label">Unclaimed Found Items</span>
    <span class="card-value"><?= (int)$counts['found_unclaimed'] ?></span>
  </div>
  <div class="card">
    <span class="card-label">Claimed Found Items</span>
    <span class="card-value"><?= (int)$counts['found_claimed'] ?></span>
  </div>
  <div class="card">
    <span class="card-label">Active Lost Reports</span>
    <span class="card-value"><?= (int)$counts['lost_missing'] ?></span>
  </div>
  <div class="card highlight">
    <span class="card-label">Pending Claims</span>
    <span class="card-value"><?= (int)$counts['claims_pending'] ?></span>
  </div>
  <div class="card">
    <span class="card-label">Approved Claims</span>
    <span class="card-value"><?= (int)$counts['claims_approved'] ?></span>
  </div>
</section>

<section class="panel">
  <h2>Recent Claims</h2>
  <?php if (empty($recentClaims)): ?>
    <p class="empty-state">No claims filed yet.</p>
  <?php else: ?>
    <table class="data-table">
      <thead>
        <tr><th>Item</th><th>Claimant</th><th>Status</th><th>Filed</th><th></th></tr>
      </thead>
      <tbody>
        <?php foreach ($recentClaims as $c): ?>
          <tr>
            <td><?= htmlspecialchars($c['item_name']) ?></td>
            <td><?= htmlspecialchars($c['first_name'] . ' ' . $c['last_name']) ?></td>
            <td><span class="badge badge-<?= strtolower($c['status']) ?>"><?= htmlspecialchars($c['status']) ?></span></td>
            <td><?= htmlspecialchars($c['claimed_at']) ?></td>
            <td><a class="link" href="claims.php">Review &rarr;</a></td>
          </tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  <?php endif; ?>
</section>
<?php require_once __DIR__ . '/includes/footer.php'; ?>
