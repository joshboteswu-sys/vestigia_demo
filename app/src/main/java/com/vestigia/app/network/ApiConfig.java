package com.vestigia.app.network;

public class ApiConfig {
    public static final String BASE_URL = "http://192.168.1.8/vestigia-api/php-api/";

    public static final String LOGIN = BASE_URL + "login.php";
    public static final String REGISTER = BASE_URL + "register.php";
    public static final String LOGOUT = BASE_URL + "logout.php";
    public static final String CHECK_SESSION = BASE_URL + "check_session.php";

    public static final String ITEMS_LIST = BASE_URL + "items_list.php";
    public static final String ITEMS_GET = BASE_URL + "items_get.php";
    public static final String ITEMS_CREATE = BASE_URL + "items_create.php";
    public static final String ITEMS_UPDATE = BASE_URL + "items_update.php";
    public static final String ITEMS_DELETE = BASE_URL + "items_delete.php";

    public static final String LOST_LIST = BASE_URL + "lost_list.php";
    public static final String LOST_GET = BASE_URL + "lost_get.php";
    public static final String LOST_CREATE = BASE_URL + "lost_create.php";
    public static final String LOST_UPDATE = BASE_URL + "lost_update.php";
    public static final String LOST_DELETE = BASE_URL + "lost_delete.php";

    public static final String CLAIMS_CREATE = BASE_URL + "claims_create.php";
    public static final String CLAIMS_STATUS = BASE_URL + "claims_status.php";
    public static final String CLAIMS_PENDING = BASE_URL + "claims_pending.php";
    public static final String CLAIMS_APPROVE = BASE_URL + "claims_approve.php";
    public static final String CLAIMS_REJECT = BASE_URL + "claims_reject.php";
    public static final String CLAIMS_APPROVED_PENDING = BASE_URL + "claims_approved_pending_handover.php";
    public static final String HANDOVERS_CREATE = BASE_URL + "handovers_create.php";

    public static final String PROFILE = BASE_URL + "profile.php";
    public static final String PROFILE_PHOTO_UPDATE = BASE_URL + "profile_photo_update.php";

    public static final String ADMIN_USERS_LIST = BASE_URL + "admin_users_list.php";
    public static final String ADMIN_TOGGLE_ADMIN = BASE_URL + "admin_toggle_admin.php";

    public static final String CHANGE_PASSWORD = BASE_URL + "change_password.php";
    public static final String RECENT_POSTS = BASE_URL + "recent_posts.php";

    public static final String VERIFY_EMAIL = BASE_URL + "verify_email.php";
    public static final String RESEND_VERIFICATION = BASE_URL + "resend_verification.php";
    public static final String NOTIFICATIONS_LIST = BASE_URL + "notifications_list.php";
    public static final String NOTIFICATIONS_READ = BASE_URL + "notifications_read.php";
    public static final String CUSTODY_RECEIVE = BASE_URL + "custody_receive.php";
    public static final String ANALYTICS = BASE_URL + "analytics.php";
    public static final String POSTS_LIST = BASE_URL + "posts_list.php";
    public static final String CLAIMS_GET = BASE_URL + "claims_get.php";
    public static final String ADMIN_SUMMARY = BASE_URL + "admin_summary.php";
    public static final String RECEIPT_GET = BASE_URL + "receipt_get.php";
    public static final String FORGOT_PASSWORD = BASE_URL + "forgot_password.php";
    public static final String RESET_PASSWORD = BASE_URL + "reset_password.php";
    public static final String LOST_RESOLVE = BASE_URL + "lost_resolve.php";
    public static final String ITEMS_DISPOSE = BASE_URL + "items_dispose.php";
    public static final String CLAIMS_WITHDRAW = BASE_URL + "claims_withdraw.php";
}