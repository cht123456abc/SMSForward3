package com.cht.smsforward;

/**
 * Enumeration representing the status of Server酱 forwarding for SMS messages
 */
public enum ServerChanForwardStatus {
    /**
     * Server酱 forwarding has not been attempted yet
     */
    NOT_SENT("not_sent"),
    
    /**
     * Server酱 forwarding is currently in progress
     */
    SENDING("sending"),
    
    /**
     * Server酱 forwarding completed successfully
     */
    SUCCESS("success"),
    
    /**
     * Server酱 forwarding failed
     */
    FAILED("failed"),
    
    /**
     * Server酱 forwarding is disabled or not applicable (no verification codes)
     */
    DISABLED("disabled");
    
    private final String value;
    
    ServerChanForwardStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Get ServerChanForwardStatus from string value
     */
    public static ServerChanForwardStatus fromValue(String value) {
        for (ServerChanForwardStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return NOT_SENT; // Default fallback
    }
    
    /**
     * Check if the status indicates a completed state (success or failed)
     */
    public boolean isCompleted() {
        return this == SUCCESS || this == FAILED;
    }
    
    /**
     * Check if the status indicates an active state (sending)
     */
    public boolean isActive() {
        return this == SENDING;
    }
    
    /**
     * Check if the status indicates a successful state
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }
    
    /**
     * Check if the status indicates a failed state
     */
    public boolean isFailed() {
        return this == FAILED;
    }
}
