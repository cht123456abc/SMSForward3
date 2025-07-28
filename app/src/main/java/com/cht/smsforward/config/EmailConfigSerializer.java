package com.cht.smsforward.config;

import android.content.SharedPreferences;

/**
 * Serializer for EmailConfig objects
 */
public class EmailConfigSerializer implements ConfigurationManager.ConfigurationSerializer<EmailConfig> {
    
    // Keys for storing email settings
    private static final String KEY_SENDER_EMAIL = "sender_email";
    private static final String KEY_SENDER_PASSWORD = "sender_password";
    private static final String KEY_RECIPIENT_EMAIL = "recipient_email";
    private static final String KEY_ENABLED = "email_enabled";
    
    private static final String[] CONFIG_KEYS = {
        KEY_SENDER_EMAIL, KEY_SENDER_PASSWORD, KEY_RECIPIENT_EMAIL, KEY_ENABLED
    };
    
    @Override
    public void serialize(EmailConfig config, SharedPreferences.Editor editor) {
        if (config.getSenderEmail() != null) {
            editor.putString(KEY_SENDER_EMAIL, config.getSenderEmail());
        }
        
        if (config.getSenderPassword() != null) {
            editor.putString(KEY_SENDER_PASSWORD, config.getSenderPassword());
        }
        
        if (config.getRecipientEmail() != null) {
            editor.putString(KEY_RECIPIENT_EMAIL, config.getRecipientEmail());
        }
        
        editor.putBoolean(KEY_ENABLED, config.isEnabled());
    }
    
    @Override
    public EmailConfig deserialize(SharedPreferences prefs) {
        String senderEmail = prefs.getString(KEY_SENDER_EMAIL, "");
        String senderPassword = prefs.getString(KEY_SENDER_PASSWORD, "");
        String recipientEmail = prefs.getString(KEY_RECIPIENT_EMAIL, "");
        boolean enabled = prefs.getBoolean(KEY_ENABLED, false);
        
        return new EmailConfig(senderEmail, senderPassword, recipientEmail, enabled);
    }
    
    @Override
    public String[] getConfigKeys() {
        return CONFIG_KEYS;
    }
}
