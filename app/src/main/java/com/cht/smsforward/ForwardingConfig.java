package com.cht.smsforward;

/**
 * Base interface for all forwarding configuration types
 */
public interface ForwardingConfig {
    /**
     * Check if the configuration is valid (has all required fields)
     */
    boolean isValid();
    
    /**
     * Check if the configuration is enabled
     */
    boolean isEnabled();
    
    /**
     * Set the enabled status
     */
    void setEnabled(boolean enabled);
    
    /**
     * Get a string representation of the configuration
     */
    String toString();
}
