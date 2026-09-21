<?php
require_once __DIR__ . '/includes/auth.php';
require_once __DIR__ . '/config/db.php';

$pageTitle = 'Users';

$users = $pdo->query('SELECT id, first_name, last_name, student_id, email, contact_number, is_admin, created_at FROM users ORDER BY created_at DESC')->fetchAll();

require_once __DIR__ . '/includes/header.php';
?>
<section class="panel">
  <table class="data-table">
    <thead>
      <tr>
        <th>Name</th><th>Student ID</th><th>Email</th><th>Contact</th>
        <th>Role</th><th>Joined</th><th>Actions</th>
      </tr>
    </thead>
    <tbody>
      <?php foreach ($users as $u): ?>
        <tr>
          <td><?= htmlspecialchars($u['first_name'] . ' ' . $u['last_name']) ?></td>
          <td><?= htmlspecialchars($u['student_id']) ?></td>
          <td><?= htmlspecialchars($u['email']) ?></td>
          <td><?= htmlspecialchars($u['contact_number'] ?? '—') ?></td>
          <td><span class="badge <?= $u['is_admin'] ? 'badge-approved' : 'badge-neutral' ?>"><?= $u['is_admin'] ? 'Admin' : 'Student' ?></span></td>
          <td><?= htmlspecialchars($u['created_at']) ?></td>
          <td class="actions-cell">
            <?php if ((int)$u['id'] !== (int)$_SESSION['admin_id']): ?>
              <form method="post" action="actions/update_user.php" class="inline-form" onsubmit="return confirm('<?= $u['is_admin'] ? 'Remove admin access for' : 'Grant admin access to' ?> <?= htmlspecialchars(addslashes($u['first_name'])) ?>?');">
                <input type="hidden" name="id" value="<?= (int)$u['id'] ?>">
                <input type="hidden" name="is_admin" value="<?= $u['is_admin'] ? 0 : 1 ?>">
                <button type="submit" class="btn-small btn-neutral"><?= $u['is_admin'] ? 'Revoke Admin' : 'Make Admin' ?></button>
              </form>
            <?php else: ?>
              <span class="muted">This is you</span>
            <?php endif; ?>
          </td>
        </tr>
      <?php endforeach; ?>
    </tbody>
  </table>
</section>
<?php require_once __DIR__ . '/includes/footer.php'; ?>
