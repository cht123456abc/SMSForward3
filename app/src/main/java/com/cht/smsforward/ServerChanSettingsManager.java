package com.cht.smsforward;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Manager for Server酱 configuration settings with encrypted storage
 */
public class ServerChanSettingsManager {
    private static final String TAG = "ServerChanSettingsManager";
    
    // Encrypted preferences file name
    private static final String PREFS_FILE_NAME = "serverchan_config_prefs";
    
    // Preference keys
    private static final String KEY_SEND_KEY = "send_key";
    private static final String KEY_ENABLED = "enabled";
    
    private Context context;
    private SharedPreferences encryptedPrefs;
    
    public ServerChanSettingsManager(Context context) {
        this.context = context;
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
            Log.e(TAG, "Failed to initialize encrypted preferences", e);
            // Fallback to regular preferences (not recommended for production)
            encryptedPrefs = context.getSharedPreferences(PREFS_FILE_NAME + "_fallback", Context.MODE_PRIVATE);
        }
    }
    
    /**
     * Save Server酱 configuration
     */
    public boolean saveServerChanConfig(ServerChanConfig config) {
        if (encryptedPrefs == null) {
            Log.e(TAG, "Encrypted preferences not initialized");
            return false;
        }
        
        if (config == null) {
            Log.e(TAG, "Cannot save null Server酱 config");
            return false;
        }
        
        try {
            SharedPreferences.Editor editor = encryptedPrefs.edit();
            
            if (config.getSendKey() != null) {
                editor.putString(KEY_SEND_KEY, config.getSendKey());
            }
            
            editor.putBoolean(KEY_ENABLED, config.isEnabled());
            
            boolean success = editor.commit();
            Log.d(TAG, "Server酱 config saved: " + success);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save Server酱 config", e);
            return false;
        }
    }
    
    /**
     * Load Server酱 configuration
     */
    public ServerChanConfig loadServerChanConfig() {
        if (encryptedPrefs == null) {
            Log.e(TAG, "Encrypted preferences not initialized");
            return new ServerChanConfig();
        }
        
        try {
            String sendKey = encryptedPrefs.getString(KEY_SEND_KEY, "");
            boolean enabled = encryptedPrefs.getBoolean(KEY_ENABLED, false);
            
            ServerChanConfig config = new ServerChanConfig(sendKey, enabled);
            Log.d(TAG, "Server酱 config loaded: " + config.toString());
            return config;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load Server酱 config", e);
            return new ServerChanConfig();
        }
    }
    
    /**
     * Clear all Server酱 configuration
     */
    public boolean clearServerChanConfig() {
        if (encryptedPrefs == null) {
            Log.e(TAG, "Encrypted preferences not initialized");
            return false;
        }
        
        try {
            SharedPreferences.Editor editor = encryptedPrefs.edit();
            editor.remove(KEY_SEND_KEY);
            editor.remove(KEY_ENABLED);
            
            boolean success = editor.commit();
            Log.d(TAG, "Server酱 config cleared: " + success);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear Server酱 config", e);
            return false;
        }
    }
    
    /**
     * Update only the enabled status
     */
    public boolean setServerChanEnabled(boolean enabled) {
        if (encryptedPrefs == null) {
            return false;
        }
        
        try {
            SharedPreferences.Editor editor = encryptedPrefs.edit();
            editor.putBoolean(KEY_ENABLED, enabled);
            return editor.commit();
        } catch (Exception e) {
            Log.e(TAG, "Failed to update Server酱 enabled status", e);
            return false;
        }
    }
    
    /**
     * Check if Server酱 is configured and enabled
     */
    public boolean isServerChanConfigured() {
        ServerChanConfig config = loadServerChanConfig();
        return config.isValid() && config.isEnabled();
    }
}
