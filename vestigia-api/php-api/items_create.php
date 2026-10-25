<?php
require_once "db.php";
require_once "auth_helper.php";
require_once "notify_helper.php";
require_once "upload_helper.php";

$pdo = getDbConnection();
$session = requireAuth($pdo);

$itemName    = trim($_POST['item_name'] ?? '');
$category    = trim($_POST['category'] ?? '');
$location    = trim($_POST['location'] ?? '');
$dateFound   = trim($_POST['date_found'] ?? '');
$description = trim($_POST['description'] ?? '');
$custodyHolder = trim($_POST['custody_holder'] ?? 'Finder');
if (!in_array($custodyHolder, ['Finder', 'Admin'])) $custodyHolder = 'Finder';
$custodyNote = trim($_POST['custody_note'] ?? '');

if ($itemName === '' || $location === '' || $dateFound === '') {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Item name, location, and date found are required."]);
    exit;
}

$imagePath = handleImageUpload('image', 'item_');

  try {
      $pdo->beginTransaction();

      $stmt = $pdo->prepare("INSERT INTO found_items (item_name, description, category, location, date_found, status, image_path, reported_by, custody_holder, custodian_id, custody_note)
                              VALUES (:name, :desc, :cat, :loc, :date, 'Active', :img, :uid, :holder, :custodian, :note) RETURNING id");
      $stmt->execute([
          "name"      => $itemName,
          "desc"      => $description,
          "cat"       => $category,
          "loc"       => $location,
          "date"      => $dateFound,
          "img"       => $imagePath,
          "uid"       => $session["user_id"],
          "holder"    => $custodyHolder,
          // Finder custody: the reporter holds it. Admin custody: the office holds it.
          "custodian" => $custodyHolder === 'Finder' ? $session["user_id"] : null,
          "note"      => $custodyNote !== '' ? $custodyNote : null
      ]);
      $newId = $stmt->fetchColumn();

      $pdo->prepare("INSERT INTO custody_log (item_id, from_holder, to_holder, to_user_id, changed_by, note)
                     VALUES (:item, NULL, :holder, :to_user, :by, :note)")
          ->execute([
              "item"    => $newId,
              "holder"  => $custodyHolder,
              "to_user" => $custodyHolder === 'Finder' ? $session["user_id"] : null,
              "by"      => $session["user_id"],
              "note"    => $custodyNote !== '' ? $custodyNote : "Initial custody when reported"
          ]);

      $pdo->commit();
  } catch (PDOException $e) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    error_log("Vestigia items_create failed: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Could not save the item right now. Please try again."]);
    exit;
}
  // Item turned in to the Admin Office: let the admins know it should be there
  if ($custodyHolder === 'Admin') {
      $reporterName = fullName($session['first_name'], $session['last_name']);
      notifyAdmins($pdo, 'item_turned_in', 'Item turned in to the Admin Office',
          "$reporterName reports turning in \"$itemName\" to the Admin Office. Please make sure it has been received.",
          $newId, null, $session['user_id']);
  }

  echo json_encode(["success" => true, "message" => "Item added successfully.", "item_id" => $newId]);