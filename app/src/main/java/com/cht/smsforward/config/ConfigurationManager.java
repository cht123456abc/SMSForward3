package com.cht.smsforward.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Unified configuration manager for all forwarding services
 * Uses encrypted shared preferences to securely store sensitive configuration data
 */
public class ConfigurationManager<T extends ForwardingConfig> {
    private static final String TAG = "ConfigurationManager";
    
    private final String prefsFileName;
    private final String configType;
    private final Supplier<T> configFactory;
    private final ConfigurationSerializer<T> serializer;
    
    private SharedPreferences encryptedPrefs;
    private Context context;
    
    /**
     * Interface for serializing/deserializing configuration objects
     */
    public interface ConfigurationSerializer<T> {
        void serialize(T config, SharedPreferences.Editor editor);
        T deserialize(SharedPreferences prefs);
        String[] getConfigKeys();
    }
    
    public ConfigurationManager(Context context, String prefsFileName, String configType, 
                               Supplier<T> configFactory, ConfigurationSerializer<T> serializer) {
        this.context = context;
        this.prefsFileName = prefsFileName;
        this.configType = configType;
        this.configFactory = configFactory;
        this.serializer = serializer;
        initializeEncryptedPreferences();
    }
    
    /**
     * Initialize encrypted shared preferences with fallback support
     */
    private void initializeEncryptedPreferences() {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    prefsFileName,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            Log.d(TAG, "Encrypted preferences initialized successfully for " + configType);
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to initialize encrypted preferences for " + configType + " - using fallback", e);
            try {
                encryptedPrefs = context.getSharedPreferences(prefsFileName + "_fallback", Context.MODE_PRIVATE);
                Log.w(TAG, "Using unencrypted fallback preferences for " + configType + " due to device-specific issue");
            } catch (Exception fallbackException) {
                Log.e(TAG, "Failed to initialize fallback preferences for " + configType, fallbackException);
                encryptedPrefs = null;
            }
        }
    }
    
    /**
     * Save configuration
     */
    public boolean saveConfig(T config) {
        if (encryptedPrefs == null) {
            Log.e(TAG, "Encrypted preferences not initialized for " + configType);
            return false;
        }
        
        if (config == null) {
            Log.e(TAG, "Cannot save null " + configType + " config");
            return false;
        }
        
        try {
            SharedPreferences.Editor editor = encryptedPrefs.edit();
            serializer.serialize(config, editor);
            
            boolean success = editor.commit();
            Log.d(TAG, configType + " config saved: " + success);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save " + configType + " config", e);
            return false;
        }
    }
    
    /**
     * Load configuration
     */
    public T loadConfig() {
        if (encryptedPrefs == null) {
            Log.e(TAG, "Encrypted preferences not initialized for " + configType);
            return configFactory.get();
        }
        
        try {
            T config = serializer.deserialize(encryptedPrefs);
            Log.d(TAG, configType + " config loaded: " + config.toString());
            return config;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load " + configType + " config", e);
            return configFactory.get();
        }
    }
    
    /**
     * Clear all configuration settings
     */
    public boolean clearConfig() {
        if (encryptedPrefs == null) {
            Log.e(TAG, "Encrypted preferences not initialized for " + configType);
            return false;
        }
        
        try {
            SharedPreferences.Editor editor = encryptedPrefs.edit();
            for (String key : serializer.getConfigKeys()) {
                editor.remove(key);
            }
            
            boolean success = editor.commit();
            Log.d(TAG, configType + " config cleared: " + success);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear " + configType + " config", e);
            return false;
        }
    }
    
    /**
     * Update only the enabled status
     */
    public boolean setEnabled(boolean enabled) {
        if (encryptedPrefs == null) {
            return false;
        }
        
        try {
            SharedPreferences.Editor editor = encryptedPrefs.edit();
            editor.putBoolean("enabled", enabled);
            boolean success = editor.commit();
            Log.d(TAG, configType + " enabled status updated: " + enabled + ", success: " + success);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to update " + configType + " enabled status", e);
            return false;
        }
    }
    
    /**
     * Check if configuration exists and is valid
     */
    public boolean hasValidConfig() {
        T config = loadConfig();
        return config.isValid() && config.isEnabled();
    }
}
