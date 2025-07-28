package com.cht.smsforward;

/**
 * Unified enumeration representing the status of message forwarding (email and Server酱)
 */
public enum ForwardStatus {
    /**
     * Forwarding has not been attempted yet
     */
    NOT_SENT("not_sent"),
    
    /**
     * Forwarding is currently in progress
     */
    SENDING("sending"),
    
    /**
     * Forwarding completed successfully
     */
    SUCCESS("success"),
    
    /**
     * Forwarding failed
     */
    FAILED("failed"),
    
    /**
     * Forwarding is disabled or not applicable (no verification codes)
     */
    DISABLED("disabled");
    
    private final String value;
    
    ForwardStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Get ForwardStatus from string value
     */
    public static ForwardStatus fromValue(String value) {
        for (ForwardStatus status : values()) {
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
