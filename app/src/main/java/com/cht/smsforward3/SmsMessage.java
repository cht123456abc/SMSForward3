package com.cht.smsforward3;

import android.text.SpannableString;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Data model representing an SMS message with verification code information
 */
public class SmsMessage {
    
    private String content;
    private String sender;
    private String packageName;
    private long timestamp;
    private List<String> verificationCodes;
    private String primaryVerificationCode;
    private SpannableString highlightedContent;
    
    public SmsMessage(String content, String sender, String packageName, long timestamp, 
                     List<String> verificationCodes, String primaryVerificationCode) {
        this.content = content;
        this.sender = sender;
        this.packageName = packageName;
        this.timestamp = timestamp;
        this.verificationCodes = verificationCodes;
        this.primaryVerificationCode = primaryVerificationCode;
        
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
    
    @Override
    public String toString() {
        return "SmsMessage{" +
                "sender='" + sender + '\'' +
                ", content='" + content + '\'' +
                ", verificationCodes=" + verificationCodes +
                ", primaryCode='" + primaryVerificationCode + '\'' +
                ", timestamp=" + getFormattedTimestamp() +
                '}';
    }
}
