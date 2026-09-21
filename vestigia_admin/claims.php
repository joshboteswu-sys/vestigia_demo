<?php
require_once __DIR__ . '/includes/auth.php';
require_once __DIR__ . '/config/db.php';

$pageTitle = 'Claims';

$statusFilter = $_GET['status'] ?? 'Pending';
$allowedStatuses = ['Pending', 'Approved', 'Rejected'];

$sql = "
    SELECT c.*, f.item_name, f.status AS item_status,
           u.first_name AS c_first, u.last_name AS c_last, u.email AS c_email
    FROM claims c
    JOIN found_items f ON f.id = c.item_id
    JOIN users u ON u.id = c.claimed_by
";
$params = [];
if (in_array($statusFilter, $allowedStatuses, true)) {
    $sql .= ' WHERE c.status = ?';
    $params[] = $statusFilter;
}
$sql .= ' ORDER BY c.claimed_at DESC';

$stmt = $pdo->prepare($sql);
$stmt->execute($params);
$claims = $stmt->fetchAll();

require_once __DIR__ . '/includes/header.php';
?>
<section class="panel">
  <div class="panel-toolbar">
    <div class="filter-tabs">
      <a class="<?= $statusFilter === '' ? 'active' : '' ?>" href="claims.php?status=">All</a>
      <a class="<?= $statusFilter === 'Pending' ? 'active' : '' ?>" href="claims.php?status=Pending">Pending</a>
      <a class="<?= $statusFilter === 'Approved' ? 'active' : '' ?>" href="claims.php?status=Approved">Approved</a>
      <a class="<?= $statusFilter === 'Rejected' ? 'active' : '' ?>" href="claims.php?status=Rejected">Rejected</a>
    </div>
  </div>

  <?php if (empty($claims)): ?>
    <p class="empty-state">No claims <?= $statusFilter ? 'with status "' . htmlspecialchars($statusFilter) . '"' : '' ?>.</p>
  <?php else: ?>
    <?php foreach ($claims as $claim): ?>
      <div class="claim-card">
        <div class="claim-main">
          <div class="claim-heading">
            <strong><?= htmlspecialchars($claim['item_name']) ?></strong>
            <span class="badge badge-<?= strtolower($claim['status']) ?>"><?= htmlspecialchars($claim['status']) ?></span>
          </div>
          <p class="claim-meta">
            Claimed by <?= htmlspecialchars($claim['c_first'] . ' ' . $claim['c_last']) ?>
            (<?= htmlspecialchars($claim['c_email']) ?>) &middot; filed <?= htmlspecialchars($claim['claimed_at']) ?>
          </p>
          <p class="claim-proof"><strong>Proof of ownership:</strong> <?= nl2br(htmlspecialchars($claim['proof_description'])) ?></p>
        </div>
        <?php if ($claim['status'] === 'Pending'): ?>
          <div class="claim-actions">
            <form method="post" action="actions/update_claim.php" class="inline-form" onsubmit="return confirm('Approve this claim and mark the item as Claimed?');">
              <input type="hidden" name="id" value="<?= (int)$claim['id'] ?>">
              <input type="hidden" name="action" value="approve">
              <button type="submit" class="btn-small btn-approve">Approve</button>
            </form>
            <form method="post" action="actions/update_claim.php" class="inline-form" onsubmit="return confirm('Reject this claim?');">
              <input type="hidden" name="id" value="<?= (int)$claim['id'] ?>">
              <input type="hidden" name="action" value="reject">
              <button type="submit" class="btn-small btn-danger">Reject</button>
            </form>
          </div>
        <?php endif; ?>
      </div>
    <?php endforeach; ?>
  <?php endif; ?>
</section>
<?php require_once __DIR__ . '/includes/footer.php'; ?>
