package com.cht.smsforward;

/**
 * Data model for email configuration settings
 */
public class EmailConfig implements ForwardingConfig {
    private String senderEmail;
    private String senderPassword;
    private String recipientEmail;
    private boolean enabled;
    
    // QQ Mail SMTP configuration constants
    public static final String QQ_SMTP_HOST = "smtp.qq.com";
    public static final int QQ_SMTP_PORT = 587; // TLS port
    public static final int QQ_SMTP_SSL_PORT = 465; // SSL port
    
    public EmailConfig() {
        this.enabled = false;
    }
    
    public EmailConfig(String senderEmail, String senderPassword, String recipientEmail, boolean enabled) {
        this.senderEmail = senderEmail;
        this.senderPassword = senderPassword;
        this.recipientEmail = recipientEmail;
        this.enabled = enabled;
    }
    
    // Getters and setters
    public String getSenderEmail() {
        return senderEmail;
    }
    
    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }
    
    public String getSenderPassword() {
        return senderPassword;
    }
    
    public void setSenderPassword(String senderPassword) {
        this.senderPassword = senderPassword;
    }
    
    public String getRecipientEmail() {
        return recipientEmail;
    }
    
    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Check if the configuration is valid for sending emails
     */
    public boolean isValid() {
        return senderEmail != null && !senderEmail.trim().isEmpty() &&
               senderPassword != null && !senderPassword.trim().isEmpty() &&
               recipientEmail != null && !recipientEmail.trim().isEmpty() &&
               isValidEmail(senderEmail) && isValidEmail(recipientEmail);
    }
    
    /**
     * Basic email validation
     */
    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
    
    /**
     * Check if this is a QQ email address
     */
    public boolean isSenderQQEmail() {
        return senderEmail != null && senderEmail.toLowerCase().contains("@qq.com");
    }
    
    @Override
    public String toString() {
        return "EmailConfig{" +
                "senderEmail='" + senderEmail + '\'' +
                ", recipientEmail='" + recipientEmail + '\'' +
                ", enabled=" + enabled +
                ", valid=" + isValid() +
                '}';
    }
}
