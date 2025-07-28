package com.cht.smsforward.config;

import android.content.Context;

/**
 * Unified settings manager that provides access to both email and ServerChan configurations
 * This replaces the separate EmailSettingsManager and ServerChanSettingsManager classes
 */
public class UnifiedSettingsManager {
    
    private final ConfigurationManager<EmailConfig> emailConfigManager;
    private final ConfigurationManager<ServerChanConfig> serverChanConfigManager;
    
    public UnifiedSettingsManager(Context context) {
        // Initialize email configuration manager
        emailConfigManager = new ConfigurationManager<>(
            context,
            "email_settings_encrypted",
            "Email",
            EmailConfig::new,
            new EmailConfigSerializer()
        );
        
        // Initialize ServerChan configuration manager
        serverChanConfigManager = new ConfigurationManager<>(
            context,
            "serverchan_config_prefs",
            "ServerChan",
            ServerChanConfig::new,
            new ServerChanConfigSerializer()
        );
    }
    
    // Email configuration methods
    public boolean saveEmailConfig(EmailConfig config) {
        return emailConfigManager.saveConfig(config);
    }
    
    public EmailConfig loadEmailConfig() {
        return emailConfigManager.loadConfig();
    }
    
    public boolean clearEmailConfig() {
        return emailConfigManager.clearConfig();
    }
    
    public boolean hasValidEmailConfig() {
        return emailConfigManager.hasValidConfig();
    }
    
    public boolean setEmailEnabled(boolean enabled) {
        return emailConfigManager.setEnabled(enabled);
    }
    
    // ServerChan configuration methods
    public boolean saveServerChanConfig(ServerChanConfig config) {
        return serverChanConfigManager.saveConfig(config);
    }
    
    public ServerChanConfig loadServerChanConfig() {
        return serverChanConfigManager.loadConfig();
    }
    
    public boolean clearServerChanConfig() {
        return serverChanConfigManager.clearConfig();
    }
    
    public boolean isServerChanConfigured() {
        return serverChanConfigManager.hasValidConfig();
    }
    
    public boolean setServerChanEnabled(boolean enabled) {
        return serverChanConfigManager.setEnabled(enabled);
    }
}
