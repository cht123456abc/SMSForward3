package com.cht.smsforward;

import android.content.SharedPreferences;

/**
 * Serializer for ServerChanConfig objects
 */
public class ServerChanConfigSerializer implements ConfigurationManager.ConfigurationSerializer<ServerChanConfig> {
    
    // Keys for storing Server酱 settings
    private static final String KEY_SEND_KEY = "send_key";
    private static final String KEY_ENABLED = "enabled";
    
    private static final String[] CONFIG_KEYS = {
        KEY_SEND_KEY, KEY_ENABLED
    };
    
    @Override
    public void serialize(ServerChanConfig config, SharedPreferences.Editor editor) {
        if (config.getSendKey() != null) {
            editor.putString(KEY_SEND_KEY, config.getSendKey());
        }
        
        editor.putBoolean(KEY_ENABLED, config.isEnabled());
    }
    
    @Override
    public ServerChanConfig deserialize(SharedPreferences prefs) {
        String sendKey = prefs.getString(KEY_SEND_KEY, "");
        boolean enabled = prefs.getBoolean(KEY_ENABLED, false);
        
        return new ServerChanConfig(sendKey, enabled);
    }
    
    @Override
    public String[] getConfigKeys() {
        return CONFIG_KEYS;
    }
}
