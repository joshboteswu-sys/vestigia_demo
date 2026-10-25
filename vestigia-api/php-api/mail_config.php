<?php
// ============================================================
// mail_config.php — email settings (edit the values below)
//
// 'log'  : emails are NOT sent; they are saved in the email_outbox table
//          (read the verification code there). Good for testing.
// 'smtp' : emails are really sent through your SMTP account.
// ============================================================
return [
    'mode'         => 'smtp',

    'host'         => 'smtp.gmail.com',
    'port'         => 587,
    'encryption'   => 'tls',                    // 'tls' for port 587, 'ssl' for port 465
    'username'     => 'josh.bote.swu@phinmaed.com',
    'password'     => 'vryx lmwx ccid qeeq',      // Gmail: an App Password, not your normal password
    'from_address' => 'josh.bote.swu@phinmaed.com',
    'from_name'    => 'Vestigia',

    // Set to false ONLY if XAMPP fails with "certificate verify failed"
    'verify_peer'  => true,

    // How long a verification code stays valid
    'code_minutes' => 10,
];