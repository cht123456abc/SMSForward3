package com.cht.smsforward;

/**
 * Enumeration representing the status of email forwarding for SMS messages
 */
public enum EmailForwardStatus {
    /**
     * Email forwarding has not been attempted yet
     */
    NOT_SENT("not_sent"),
    
    /**
     * Email forwarding is currently in progress
     */
    SENDING("sending"),
    
    /**
     * Email forwarding completed successfully
     */
    SUCCESS("success"),
    
    /**
     * Email forwarding failed
     */
    FAILED("failed"),
    
    /**
     * Email forwarding is disabled or not applicable (no verification codes)
     */
    DISABLED("disabled");
    
    private final String value;
    
    EmailForwardStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Get EmailForwardStatus from string value
     */
    public static EmailForwardStatus fromValue(String value) {
        for (EmailForwardStatus status : values()) {
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
