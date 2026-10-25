<?php
// ============================================================
// upload_helper.php — saves uploaded images after checking their real content
// ============================================================

const UPLOAD_MAX_BYTES = 8 * 1024 * 1024; // 8 MB

function uploadFail($status, $message) {
    http_response_code($status);
    echo json_encode(["success" => false, "message" => $message]);
    exit;
}

// Returns "uploads/<file>" for a valid image, or null when no file was sent.
// Any other problem ends the request with a clear JSON error.
function handleImageUpload($field, $prefix = 'img_') {
    if (!isset($_FILES[$field]) || $_FILES[$field]['error'] === UPLOAD_ERR_NO_FILE) return null;

    $file = $_FILES[$field];

    if ($file['error'] === UPLOAD_ERR_INI_SIZE || $file['error'] === UPLOAD_ERR_FORM_SIZE) {
        uploadFail(413, "The image is too large (max 8 MB).");
    }
    if ($file['error'] !== UPLOAD_ERR_OK) {
        uploadFail(400, "The image could not be uploaded. Please try again.");
    }
    if ($file['size'] > UPLOAD_MAX_BYTES) {
        uploadFail(413, "The image is too large (max 8 MB).");
    }
    if (!is_uploaded_file($file['tmp_name'])) {
        uploadFail(400, "Invalid upload.");
    }

    // The type comes from the file's own header, never from its name
    $info = @getimagesize($file['tmp_name']);
    $allowed = [IMAGETYPE_JPEG => 'jpg', IMAGETYPE_PNG => 'png'];
    if (defined('IMAGETYPE_WEBP')) $allowed[IMAGETYPE_WEBP] = 'webp';

    if ($info === false || !isset($allowed[$info[2]])) {
        uploadFail(400, "Only real JPG, PNG or WEBP images are allowed.");
    }
    if ($info[0] > 12000 || $info[1] > 12000) {
        uploadFail(400, "The image dimensions are too large.");
    }

    $dir = defined('UPLOAD_DIR') ? UPLOAD_DIR : __DIR__ . '/uploads';
    if (!is_dir($dir) && !mkdir($dir, 0775, true)) {
        error_log("Vestigia: could not create upload directory $dir");
        uploadFail(500, "The server could not store the image.");
    }

    $prefix = preg_replace('/[^a-z0-9_]/i', '', $prefix);
    $name = $prefix . bin2hex(random_bytes(8)) . '.' . $allowed[$info[2]];

    if (!move_uploaded_file($file['tmp_name'], rtrim($dir, '/') . '/' . $name)) {
        error_log("Vestigia: move_uploaded_file failed for $name");
        uploadFail(500, "The server could not store the image.");
    }

    return 'uploads/' . $name;
}