package com.cht.smsforward;

import android.text.TextUtils;

/**
 * Data model for Server酱 configuration settings
 */
public class ServerChanConfig {
    private String sendKey;
    private boolean enabled;
    
    // Server酱 API configuration constants
    public static final String SERVERCHAN_API_URL = "https://sctapi.ftqq.com/";
    public static final String SERVERCHAN_SEND_ENDPOINT = "send";
    
    public ServerChanConfig() {
        this.enabled = false;
    }
    
    public ServerChanConfig(String sendKey, boolean enabled) {
        this.sendKey = sendKey;
        this.enabled = enabled;
    }
    
    // Getters and setters
    public String getSendKey() {
        return sendKey;
    }
    
    public void setSendKey(String sendKey) {
        this.sendKey = sendKey;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Check if the configuration is valid (has all required fields)
     */
    public boolean isValid() {
        return !TextUtils.isEmpty(sendKey) && sendKey.trim().length() > 0;
    }
    
    /**
     * Get the complete API URL for sending messages
     */
    public String getApiUrl() {
        if (TextUtils.isEmpty(sendKey)) {
            return null;
        }
        return SERVERCHAN_API_URL + sendKey + ".send";
    }
    
    @Override
    public String toString() {
        return "ServerChanConfig{" +
                "sendKey='" + (sendKey != null ? "[PROTECTED]" : "null") + '\'' +
                ", enabled=" + enabled +
                ", valid=" + isValid() +
                '}';
    }
    
    /**
     * Create a copy of this configuration
     */
    public ServerChanConfig copy() {
        return new ServerChanConfig(this.sendKey, this.enabled);
    }
    
    /**
     * Clear all configuration data
     */
    public void clear() {
        this.sendKey = null;
        this.enabled = false;
    }
}
