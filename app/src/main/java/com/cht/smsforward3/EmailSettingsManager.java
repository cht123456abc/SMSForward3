package com.cht.smsforward3;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Secure storage manager for email configuration settings
 * Uses EncryptedSharedPreferences to protect sensitive credentials
 */
public class EmailSettingsManager {
    private static final String TAG = "EmailSettingsManager";
    private static final String PREFS_FILE_NAME = "email_settings_encrypted";
    
    // Keys for storing email settings
    private static final String KEY_SENDER_EMAIL = "sender_email";
    private static final String KEY_SENDER_PASSWORD = "sender_password";
    private static final String KEY_RECIPIENT_EMAIL = "recipient_email";
    private static final String KEY_ENABLED = "email_enabled";
    
    private SharedPreferences encryptedPrefs;
    private Context context;
    
    public EmailSettingsManager(Context context) {
        this.context = context.getApplicationContext();
        initializeEncryptedPreferences();
    }
    
    /**
     * Initialize encrypted shared preferences
     */
    private void initializeEncryptedPreferences() {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            
            encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            
            Log.d(TAG, "Encrypted preferences initialized successfully");
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to initialize encrypted preferences - using fallback", e);
            // Fallback to regular SharedPreferences (not recommended for production)
            try {
                encryptedPrefs = context.getSharedPreferences(PREFS_FILE_NAME + "_fallback", Context.MODE_PRIVATE);
                Log.w(TAG, "Using unencrypted fallback preferences for email settings");
            } catch (Exception fallbackException) {
                Log.e(TAG, "Failed to initialize fallback preferences", fallbackException);
                encryptedPrefs = null;
            }
        }
    }
    
    /**
     * Save email configuration
     */
    public boolean saveEmailConfig(EmailConfig config) {
        if (encryptedPrefs == null) {
            Log.e(TAG, "Encrypted preferences not initialized");
            return false;
        }
        
        try {
            SharedPreferences.Editor editor = encryptedPrefs.edit();
            
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
            
            boolean success = editor.commit();
            Log.d(TAG, "Email config saved: " + success);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save email config", e);
            return false;
        }
    }
    
    /**
     * Load email configuration
     */
    public EmailConfig loadEmailConfig() {
        if (encryptedPrefs == null) {
            Log.e(TAG, "Encrypted preferences not initialized");
            return new EmailConfig();
        }
        
        try {
            String senderEmail = encryptedPrefs.getString(KEY_SENDER_EMAIL, "");
            String senderPassword = encryptedPrefs.getString(KEY_SENDER_PASSWORD, "");
            String recipientEmail = encryptedPrefs.getString(KEY_RECIPIENT_EMAIL, "");
            boolean enabled = encryptedPrefs.getBoolean(KEY_ENABLED, false);
            
            EmailConfig config = new EmailConfig(senderEmail, senderPassword, recipientEmail, enabled);
            Log.d(TAG, "Email config loaded: " + config.toString());
            return config;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load email config", e);
            return new EmailConfig();
        }
    }
    
    /**
     * Clear all email settings
     */
    public boolean clearEmailConfig() {
        if (encryptedPrefs == null) {
            Log.e(TAG, "Encrypted preferences not initialized");
            return false;
        }
        
        try {
            SharedPreferences.Editor editor = encryptedPrefs.edit();
            editor.remove(KEY_SENDER_EMAIL);
            editor.remove(KEY_SENDER_PASSWORD);
            editor.remove(KEY_RECIPIENT_EMAIL);
            editor.remove(KEY_ENABLED);
            
            boolean success = editor.commit();
            Log.d(TAG, "Email config cleared: " + success);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear email config", e);
            return false;
        }
    }
    
    /**
     * Check if email configuration exists and is valid
     */
    public boolean hasValidEmailConfig() {
        EmailConfig config = loadEmailConfig();
        return config.isValid() && config.isEnabled();
    }
    
    /**
     * Update only the enabled status
     */
    public boolean setEmailEnabled(boolean enabled) {
        if (encryptedPrefs == null) {
            return false;
        }
        
        try {
            SharedPreferences.Editor editor = encryptedPrefs.edit();
            editor.putBoolean(KEY_ENABLED, enabled);
            return editor.commit();
        } catch (Exception e) {
            Log.e(TAG, "Failed to update email enabled status", e);
            return false;
        }
    }
}
