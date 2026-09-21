<?php
require_once __DIR__ . '/includes/auth.php';
require_once __DIR__ . '/config/db.php';

$pageTitle = 'Found Items';

$statusFilter = $_GET['status'] ?? '';
$allowedStatuses = ['Unclaimed', 'Claimed'];

$sql = "
    SELECT f.*, u.first_name, u.last_name
    FROM found_items f
    LEFT JOIN users u ON u.id = f.reported_by
";
$params = [];
if (in_array($statusFilter, $allowedStatuses, true)) {
    $sql .= ' WHERE f.status = ?';
    $params[] = $statusFilter;
}
$sql .= ' ORDER BY f.date_found DESC, f.id DESC';

$stmt = $pdo->prepare($sql);
$stmt->execute($params);
$items = $stmt->fetchAll();

require_once __DIR__ . '/includes/header.php';
?>
<section class="panel">
  <div class="panel-toolbar">
    <div class="filter-tabs">
      <a class="<?= $statusFilter === '' ? 'active' : '' ?>" href="found_items.php">All</a>
      <a class="<?= $statusFilter === 'Unclaimed' ? 'active' : '' ?>" href="found_items.php?status=Unclaimed">Unclaimed</a>
      <a class="<?= $statusFilter === 'Claimed' ? 'active' : '' ?>" href="found_items.php?status=Claimed">Claimed</a>
    </div>
  </div>

  <?php if (empty($items)): ?>
    <p class="empty-state">No found items <?= $statusFilter ? 'with status "' . htmlspecialchars($statusFilter) . '"' : '' ?>.</p>
  <?php else: ?>
    <table class="data-table">
      <thead>
        <tr>
          <th>Item</th><th>Category</th><th>Location</th><th>Date Found</th>
          <th>Reported By</th><th>Status</th><th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <?php foreach ($items as $item): ?>
          <tr>
            <td>
              <strong><?= htmlspecialchars($item['item_name']) ?></strong>
              <?php if ($item['description']): ?>
                <div class="muted"><?= htmlspecialchars($item['description']) ?></div>
              <?php endif; ?>
            </td>
            <td><?= htmlspecialchars($item['category'] ?? '—') ?></td>
            <td><?= htmlspecialchars($item['location']) ?></td>
            <td><?= htmlspecialchars($item['date_found']) ?></td>
            <td><?= $item['first_name'] ? htmlspecialchars($item['first_name'] . ' ' . $item['last_name']) : '<span class="muted">—</span>' ?></td>
            <td><span class="badge badge-<?= strtolower($item['status']) ?>"><?= htmlspecialchars($item['status']) ?></span></td>
            <td class="actions-cell">
              <form method="post" action="actions/update_found_item.php" class="inline-form">
                <input type="hidden" name="id" value="<?= (int)$item['id'] ?>">
                <?php if ($item['status'] === 'Unclaimed'): ?>
                  <input type="hidden" name="status" value="Claimed">
                  <button type="submit" class="btn-small btn-approve">Mark Claimed</button>
                <?php else: ?>
                  <input type="hidden" name="status" value="Unclaimed">
                  <button type="submit" class="btn-small btn-neutral">Reopen</button>
                <?php endif; ?>
              </form>
              <form method="post" action="actions/update_found_item.php" class="inline-form" onsubmit="return confirm('Delete this item permanently? This also deletes any related claims.');">
                <input type="hidden" name="id" value="<?= (int)$item['id'] ?>">
                <input type="hidden" name="delete" value="1">
                <button type="submit" class="btn-small btn-danger">Delete</button>
              </form>
            </td>
          </tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  <?php endif; ?>
</section>
<?php require_once __DIR__ . '/includes/footer.php'; ?>
