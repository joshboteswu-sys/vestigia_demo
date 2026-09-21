<?php
/**
 * Include at the very top of every protected admin page.
 * Uses PHP's native session, separate from the Android app's
 * token-based `sessions` table (that one is for the mobile API;
 * this is a plain server-side session for the browser admin panel).
 */
session_start();

if (empty($_SESSION['admin_id'])) {
    header('Location: login.php');
    exit;
}
