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
    private EmailForwardStatus emailForwardStatus;
    private String emailForwardError;

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

        // Initialize email forward status
        this.emailForwardStatus = hasVerificationCodes() ? EmailForwardStatus.NOT_SENT : EmailForwardStatus.DISABLED;
        this.emailForwardError = null;

        // Create highlighted content
        this.highlightedContent = VerificationCodeExtractor.createHighlightedText(content);
    }

    /**
     * Constructor with email forward status (for loading from storage)
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
        this.emailForwardStatus = emailForwardStatus != null ? emailForwardStatus :
            (hasVerificationCodes() ? EmailForwardStatus.NOT_SENT : EmailForwardStatus.DISABLED);
        this.emailForwardError = emailForwardError;

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

    public EmailForwardStatus getEmailForwardStatus() {
        return emailForwardStatus;
    }

    public void setEmailForwardStatus(EmailForwardStatus emailForwardStatus) {
        this.emailForwardStatus = emailForwardStatus;
    }

    public String getEmailForwardError() {
        return emailForwardError;
    }

    public void setEmailForwardError(String emailForwardError) {
        this.emailForwardError = emailForwardError;
    }

    /**
     * Update email forward status to sending
     */
    public void setEmailSending() {
        this.emailForwardStatus = EmailForwardStatus.SENDING;
        this.emailForwardError = null;
    }

    /**
     * Update email forward status to success
     */
    public void setEmailSent() {
        this.emailForwardStatus = EmailForwardStatus.SUCCESS;
        this.emailForwardError = null;
    }

    /**
     * Update email forward status to failed with error message
     */
    public void setEmailFailed(String error) {
        this.emailForwardStatus = EmailForwardStatus.FAILED;
        this.emailForwardError = error;
    }
    
    @Override
    public String toString() {
        return "SmsMessage{" +
                "sender='" + sender + '\'' +
                ", content='" + content + '\'' +
                ", verificationCodes=" + verificationCodes +
                ", primaryCode='" + primaryVerificationCode + '\'' +
                ", timestamp=" + getFormattedTimestamp() +
                ", emailStatus=" + emailForwardStatus +
                ", emailError='" + emailForwardError + '\'' +
                '}';
    }
}
