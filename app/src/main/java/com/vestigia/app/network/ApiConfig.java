package com.vestigia.app.network;

public class ApiConfig {
    // Emulator talking to a PHP server running on your own PC via XAMPP/WAMP:
    public static final String BASE_URL = "http://192.168.1.5/vestigia-api/php-api/";

    // If testing on a real device on the same WiFi network, replace with your PC's LAN IP, e.g.:
    // public static final String BASE_URL = "http://192.168.1.5/vestigia-api/";

    public static final String LOGIN = BASE_URL + "login.php";
    public static final String REGISTER = BASE_URL + "register.php";
    public static final String LOGOUT = BASE_URL + "logout.php";
    public static final String CHECK_SESSION = BASE_URL + "check_session.php";
    public static final String ITEMS_LIST = BASE_URL + "items_list.php";
    public static final String ITEMS_GET = BASE_URL + "items_get.php";
    public static final String ITEMS_CREATE = BASE_URL + "items_create.php";
    public static final String ITEMS_UPDATE = BASE_URL + "items_update.php";
    public static final String ITEMS_DELETE = BASE_URL + "items_delete.php";
}
