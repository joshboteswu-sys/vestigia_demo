<?php
require_once __DIR__ . '/includes/auth.php';
require_once __DIR__ . '/config/db.php';

$pageTitle = 'Lost Items';

$statusFilter = $_GET['status'] ?? '';
$allowedStatuses = ['Missing', 'Found', 'Closed'];

$sql = "
    SELECT l.*, u.first_name, u.last_name
    FROM lost_items l
    LEFT JOIN users u ON u.id = l.reported_by
";
$params = [];
if (in_array($statusFilter, $allowedStatuses, true)) {
    $sql .= ' WHERE l.status = ?';
    $params[] = $statusFilter;
}
$sql .= ' ORDER BY l.date_lost DESC, l.id DESC';

$stmt = $pdo->prepare($sql);
$stmt->execute($params);
$items = $stmt->fetchAll();

require_once __DIR__ . '/includes/header.php';
?>
<section class="panel">
  <div class="panel-toolbar">
    <div class="filter-tabs">
      <a class="<?= $statusFilter === '' ? 'active' : '' ?>" href="lost_items.php">All</a>
      <a class="<?= $statusFilter === 'Missing' ? 'active' : '' ?>" href="lost_items.php?status=Missing">Missing</a>
      <a class="<?= $statusFilter === 'Found' ? 'active' : '' ?>" href="lost_items.php?status=Found">Found</a>
      <a class="<?= $statusFilter === 'Closed' ? 'active' : '' ?>" href="lost_items.php?status=Closed">Closed</a>
    </div>
  </div>

  <?php if (empty($items)): ?>
    <p class="empty-state">No lost item reports <?= $statusFilter ? 'with status "' . htmlspecialchars($statusFilter) . '"' : '' ?>.</p>
  <?php else: ?>
    <table class="data-table">
      <thead>
        <tr>
          <th>Item</th><th>Category</th><th>Last Seen</th><th>Date Lost</th>
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
            <td><?= htmlspecialchars($item['last_seen_location']) ?></td>
            <td><?= htmlspecialchars($item['date_lost']) ?></td>
            <td><?= $item['first_name'] ? htmlspecialchars($item['first_name'] . ' ' . $item['last_name']) : '<span class="muted">—</span>' ?></td>
            <td><span class="badge badge-<?= strtolower($item['status']) ?>"><?= htmlspecialchars($item['status']) ?></span></td>
            <td class="actions-cell">
              <form method="post" action="actions/update_lost_item.php" class="inline-form">
                <input type="hidden" name="id" value="<?= (int)$item['id'] ?>">
                <select name="status" class="status-select" onchange="this.form.submit()">
                  <?php foreach ($allowedStatuses as $s): ?>
                    <option value="<?= $s ?>" <?= $s === $item['status'] ? 'selected' : '' ?>><?= $s ?></option>
                  <?php endforeach; ?>
                </select>
                <noscript><button type="submit" class="btn-small btn-neutral">Update</button></noscript>
              </form>
              <form method="post" action="actions/update_lost_item.php" class="inline-form" onsubmit="return confirm('Delete this report permanently?');">
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
