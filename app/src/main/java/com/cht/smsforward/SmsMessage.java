package com.cht.smsforward;

import android.text.SpannableString;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Data model representing an SMS message with verification code information
 */
public class SmsMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private String content;
    private String sender;
    private String packageName;
    private long timestamp;
    private List<String> verificationCodes;
    private String primaryVerificationCode;
    private transient SpannableString highlightedContent; // transient to exclude from JSON serialization

    // Unified forwarding status (single status for all forwarding methods)
    private ForwardStatus forwardStatus;
    private String forwardError;

    // Internal tracking for individual forwarding methods (not exposed in UI)
    private transient ForwardStatus emailStatus;
    private transient String emailError;
    private transient ForwardStatus serverChanStatus;
    private transient String serverChanError;

    // Legacy fields for backward compatibility (deprecated)
    @Deprecated
    private EmailForwardStatus legacyEmailStatus;
    @Deprecated
    private ServerChanForwardStatus legacyServerChanStatus;
    @Deprecated
    private ForwardStatus emailForwardStatus;
    @Deprecated
    private String emailForwardError;
    @Deprecated
    private ForwardStatus serverChanForwardStatus;
    @Deprecated
    private String serverChanForwardError;

    /**
     * Default constructor for Gson deserialization
     */
    public SmsMessage() {
        // Default constructor for Gson
    }

    public SmsMessage(String content, String sender, String packageName, long timestamp,
                     List<String> verificationCodes, String primaryVerificationCode) {
        this.content = content;
        this.sender = sender;
        this.packageName = packageName;
        this.timestamp = timestamp;
        this.verificationCodes = verificationCodes;
        this.primaryVerificationCode = primaryVerificationCode;

        // Initialize unified forwarding status
        this.forwardStatus = hasVerificationCodes() ? ForwardStatus.NOT_SENT : ForwardStatus.DISABLED;
        this.forwardError = null;

        // Initialize internal tracking (transient)
        this.emailStatus = hasVerificationCodes() ? ForwardStatus.NOT_SENT : ForwardStatus.DISABLED;
        this.emailError = null;
        this.serverChanStatus = hasVerificationCodes() ? ForwardStatus.NOT_SENT : ForwardStatus.DISABLED;
        this.serverChanError = null;

        // Create highlighted content
        this.highlightedContent = VerificationCodeExtractor.createHighlightedText(content);
    }

    /**
     * Constructor with email forward status (for loading from storage - backward compatibility)
     */
    public SmsMessage(String content, String sender, String packageName, long timestamp,
                     List<String> verificationCodes, String primaryVerificationCode,
                     EmailForwardStatus emailForwardStatus, String emailForwardError) {
        this.content = content;
        this.sender = sender;
        this.packageName = packageName;
        this.timestamp = timestamp;
        this.verificationCodes = verificationCodes;
        this.primaryVerificationCode = primaryVerificationCode;

        // Convert legacy status and initialize unified status
        this.emailStatus = convertLegacyEmailStatus(emailForwardStatus);
        this.emailError = emailForwardError;
        this.serverChanStatus = hasVerificationCodes() ? ForwardStatus.NOT_SENT : ForwardStatus.DISABLED;
        this.serverChanError = null;

        // Calculate unified status
        this.forwardStatus = calculateUnifiedStatus();
        this.forwardError = calculateUnifiedError();

        // Create highlighted content
        this.highlightedContent = VerificationCodeExtractor.createHighlightedText(content);
    }

    /**
     * Constructor with both email and Server酱 forward status (for loading from storage - backward compatibility)
     */
    public SmsMessage(String content, String sender, String packageName, long timestamp,
                     List<String> verificationCodes, String primaryVerificationCode,
                     EmailForwardStatus emailForwardStatus, String emailForwardError,
                     ServerChanForwardStatus serverChanForwardStatus, String serverChanForwardError) {
        this.content = content;
        this.sender = sender;
        this.packageName = packageName;
        this.timestamp = timestamp;
        this.verificationCodes = verificationCodes;
        this.primaryVerificationCode = primaryVerificationCode;

        // Convert legacy statuses and initialize internal tracking
        this.emailStatus = convertLegacyEmailStatus(emailForwardStatus);
        this.emailError = emailForwardError;
        this.serverChanStatus = convertLegacyServerChanStatus(serverChanForwardStatus);
        this.serverChanError = serverChanForwardError;

        // Calculate unified status
        this.forwardStatus = calculateUnifiedStatus();
        this.forwardError = calculateUnifiedError();

        // Create highlighted content
        this.highlightedContent = VerificationCodeExtractor.createHighlightedText(content);
    }

    /**
     * Constructor with unified forward status (new preferred constructor)
     */
    public SmsMessage(String content, String sender, String packageName, long timestamp,
                     List<String> verificationCodes, String primaryVerificationCode,
                     ForwardStatus forwardStatus, String forwardError) {
        this.content = content;
        this.sender = sender;
        this.packageName = packageName;
        this.timestamp = timestamp;
        this.verificationCodes = verificationCodes;
        this.primaryVerificationCode = primaryVerificationCode;

        // Set unified status
        this.forwardStatus = forwardStatus != null ? forwardStatus :
            (hasVerificationCodes() ? ForwardStatus.NOT_SENT : ForwardStatus.DISABLED);
        this.forwardError = forwardError;

        // Initialize internal tracking (will be updated as forwarding progresses)
        this.emailStatus = hasVerificationCodes() ? ForwardStatus.NOT_SENT : ForwardStatus.DISABLED;
        this.emailError = null;
        this.serverChanStatus = hasVerificationCodes() ? ForwardStatus.NOT_SENT : ForwardStatus.DISABLED;
        this.serverChanError = null;

        // Create highlighted content
        this.highlightedContent = VerificationCodeExtractor.createHighlightedText(content);
    }
    
    // Getters
    public String getContent() {
        return content;
    }
    
    public String getSender() {
        return sender;
    }
    
    public String getPackageName() {
        return packageName;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public List<String> getVerificationCodes() {
        return verificationCodes;
    }
    
    public String getPrimaryVerificationCode() {
        return primaryVerificationCode;
    }
    
    public SpannableString getHighlightedContent() {
        if (highlightedContent == null && content != null) {
            // Recreate highlighted content if it's null (e.g., after deserialization)
            highlightedContent = VerificationCodeExtractor.createHighlightedText(content);
        }
        return highlightedContent;
    }
    
    public boolean hasVerificationCodes() {
        return verificationCodes != null && !verificationCodes.isEmpty();
    }
    
    public String getFormattedTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // Unified forwarding status methods
    public ForwardStatus getForwardStatus() {
        return forwardStatus;
    }

    public String getForwardError() {
        return forwardError;
    }

    // Legacy methods for backward compatibility
    @Deprecated
    public ForwardStatus getEmailForwardStatus() {
        return forwardStatus; // Return unified status for backward compatibility
    }

    @Deprecated
    public void setEmailForwardStatus(ForwardStatus emailForwardStatus) {
        // For backward compatibility, update internal email status and recalculate unified status
        this.emailStatus = emailForwardStatus;
        updateUnifiedStatus();
    }

    @Deprecated
    public void setEmailForwardStatus(EmailForwardStatus emailForwardStatus) {
        this.emailStatus = convertLegacyEmailStatus(emailForwardStatus);
        updateUnifiedStatus();
    }

    @Deprecated
    public String getEmailForwardError() {
        return forwardError; // Return unified error for backward compatibility
    }

    @Deprecated
    public void setEmailForwardError(String emailForwardError) {
        this.emailError = emailForwardError;
        updateUnifiedStatus();
    }

    /**
     * Update email forward status to sending
     */
    public void setEmailSending() {
        this.emailStatus = ForwardStatus.SENDING;
        this.emailError = null;
        updateUnifiedStatus();
    }

    /**
     * Update email forward status to success
     */
    public void setEmailSent() {
        this.emailStatus = ForwardStatus.SUCCESS;
        this.emailError = null;
        updateUnifiedStatus();
    }

    /**
     * Update email forward status to failed with error message
     */
    public void setEmailFailed(String error) {
        this.emailStatus = ForwardStatus.FAILED;
        this.emailError = error;
        updateUnifiedStatus();
    }

    // Legacy Server酱 methods for backward compatibility
    @Deprecated
    public ForwardStatus getServerChanForwardStatus() {
        return forwardStatus; // Return unified status for backward compatibility
    }

    @Deprecated
    public void setServerChanForwardStatus(ForwardStatus serverChanForwardStatus) {
        this.serverChanStatus = serverChanForwardStatus;
        updateUnifiedStatus();
    }

    @Deprecated
    public void setServerChanForwardStatus(ServerChanForwardStatus serverChanForwardStatus) {
        this.serverChanStatus = convertLegacyServerChanStatus(serverChanForwardStatus);
        updateUnifiedStatus();
    }

    @Deprecated
    public String getServerChanForwardError() {
        return forwardError; // Return unified error for backward compatibility
    }

    @Deprecated
    public void setServerChanForwardError(String serverChanForwardError) {
        this.serverChanError = serverChanForwardError;
        updateUnifiedStatus();
    }

    /**
     * Update Server酱 forward status to sending
     */
    public void setServerChanSending() {
        this.serverChanStatus = ForwardStatus.SENDING;
        this.serverChanError = null;
        updateUnifiedStatus();
    }

    /**
     * Update Server酱 forward status to success
     */
    public void setServerChanSent() {
        this.serverChanStatus = ForwardStatus.SUCCESS;
        this.serverChanError = null;
        updateUnifiedStatus();
    }

    /**
     * Update Server酱 forward status to failed with error message
     */
    public void setServerChanFailed(String error) {
        this.serverChanStatus = ForwardStatus.FAILED;
        this.serverChanError = error;
        updateUnifiedStatus();
    }
    
    /**
     * Update unified status based on individual forwarding method statuses
     */
    private void updateUnifiedStatus() {
        this.forwardStatus = calculateUnifiedStatus();
        this.forwardError = calculateUnifiedError();
    }

    /**
     * Calculate unified forwarding status based on email and Server酱 statuses
     */
    private ForwardStatus calculateUnifiedStatus() {
        // If no verification codes, forwarding is disabled
        if (!hasVerificationCodes()) {
            return ForwardStatus.DISABLED;
        }

        // Initialize with default values if null
        ForwardStatus email = this.emailStatus != null ? this.emailStatus : ForwardStatus.NOT_SENT;
        ForwardStatus serverChan = this.serverChanStatus != null ? this.serverChanStatus : ForwardStatus.NOT_SENT;

        // If any method is currently sending, overall status is sending
        if (email == ForwardStatus.SENDING || serverChan == ForwardStatus.SENDING) {
            return ForwardStatus.SENDING;
        }

        // If at least one method succeeded, overall status is success
        if (email == ForwardStatus.SUCCESS || serverChan == ForwardStatus.SUCCESS) {
            return ForwardStatus.SUCCESS;
        }

        // If both methods failed, overall status is failed
        if (email == ForwardStatus.FAILED && serverChan == ForwardStatus.FAILED) {
            return ForwardStatus.FAILED;
        }

        // If one failed and one not sent, overall status is failed (partial failure)
        if ((email == ForwardStatus.FAILED && serverChan == ForwardStatus.NOT_SENT) ||
            (email == ForwardStatus.NOT_SENT && serverChan == ForwardStatus.FAILED)) {
            return ForwardStatus.FAILED;
        }

        // Default to not sent
        return ForwardStatus.NOT_SENT;
    }

    /**
     * Calculate unified error message based on individual errors
     */
    private String calculateUnifiedError() {
        StringBuilder errorBuilder = new StringBuilder();

        if (this.emailError != null && !this.emailError.trim().isEmpty()) {
            errorBuilder.append("邮件: ").append(this.emailError);
        }

        if (this.serverChanError != null && !this.serverChanError.trim().isEmpty()) {
            if (errorBuilder.length() > 0) {
                errorBuilder.append("; ");
            }
            errorBuilder.append("Server酱: ").append(this.serverChanError);
        }

        return errorBuilder.length() > 0 ? errorBuilder.toString() : null;
    }

    /**
     * Convert legacy EmailForwardStatus to unified ForwardStatus
     */
    private ForwardStatus convertLegacyEmailStatus(EmailForwardStatus legacyStatus) {
        if (legacyStatus == null) {
            return hasVerificationCodes() ? ForwardStatus.NOT_SENT : ForwardStatus.DISABLED;
        }

        switch (legacyStatus) {
            case NOT_SENT: return ForwardStatus.NOT_SENT;
            case SENDING: return ForwardStatus.SENDING;
            case SUCCESS: return ForwardStatus.SUCCESS;
            case FAILED: return ForwardStatus.FAILED;
            case DISABLED: return ForwardStatus.DISABLED;
            default: return ForwardStatus.NOT_SENT;
        }
    }

    /**
     * Convert legacy ServerChanForwardStatus to unified ForwardStatus
     */
    private ForwardStatus convertLegacyServerChanStatus(ServerChanForwardStatus legacyStatus) {
        if (legacyStatus == null) {
            return hasVerificationCodes() ? ForwardStatus.NOT_SENT : ForwardStatus.DISABLED;
        }

        switch (legacyStatus) {
            case NOT_SENT: return ForwardStatus.NOT_SENT;
            case SENDING: return ForwardStatus.SENDING;
            case SUCCESS: return ForwardStatus.SUCCESS;
            case FAILED: return ForwardStatus.FAILED;
            case DISABLED: return ForwardStatus.DISABLED;
            default: return ForwardStatus.NOT_SENT;
        }
    }

    @Override
    public String toString() {
        return "SmsMessage{" +
                "sender='" + sender + '\'' +
                ", content='" + content + '\'' +
                ", verificationCodes=" + verificationCodes +
                ", primaryCode='" + primaryVerificationCode + '\'' +
                ", timestamp=" + getFormattedTimestamp() +
                ", forwardStatus=" + forwardStatus +
                ", forwardError='" + forwardError + '\'' +
                ", emailStatus=" + emailStatus +
                ", serverChanStatus=" + serverChanStatus +
                '}';
    }
}
