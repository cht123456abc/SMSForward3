package com.cht.smsforward;

import android.app.ActivityManager;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationListenerService to intercept SMS notifications from system SMS apps
 * This service captures SMS content without requiring traditional SMS permissions
 */
public class SmsNotificationListener extends NotificationListenerService {

    private static final String TAG = "SmsNotificationListener";

    private SmsDataManager smsDataManager;
    private EmailSender emailSender;
    private ServerChanSender serverChanSender;
    private MessageQueue messageQueue;

    // Common SMS app package names for Android and Meizu devices
    private static final String[] SMS_PACKAGES = {
        "com.android.mms",           // Default Android Messages
        "com.google.android.apps.messaging", // Google Messages
        "com.meizu.flyme.mms",       // Meizu SMS app (primary target)
        "com.meizu.mms",             // Alternative Meizu SMS package
        "com.samsung.android.messaging", // Samsung Messages
        "com.android.messaging",     // AOSP Messaging
        "com.sonyericsson.conversations", // Sony Messages
        "com.htc.sense.mms"          // HTC Messages
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "=== SMS NOTIFICATION LISTENER SERVICE CREATED ===");
        Log.d(TAG, "Service package: " + getPackageName());
        Log.d(TAG, "Supported SMS packages: " + java.util.Arrays.toString(SMS_PACKAGES));

        // Initialize components for direct processing
        smsDataManager = new SmsDataManager(this);
        emailSender = new EmailSender(this);
        serverChanSender = new ServerChanSender(this);
        messageQueue = new MessageQueue(this);
    }
    
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        Log.e(TAG, "=== NOTIFICATION POSTED ===");
        Log.e(TAG, "Package: " + sbn.getPackageName());
        Log.e(TAG, "ID: " + sbn.getId());
        Log.e(TAG, "Tag: " + sbn.getTag());
        Log.e(TAG, "Post time: " + sbn.getPostTime());

        // Check if notification is from an SMS app
        if (!isSmsNotification(sbn)) {
            Log.e(TAG, "Not an SMS notification - ignoring package: " + sbn.getPackageName());
            return;
        }

        Log.e(TAG, "✅ SMS notification detected from: " + sbn.getPackageName());

        // Additional SMS validation checks
        if (!isValidSmsNotification(sbn)) {
            Log.e(TAG, "Notification filtered out - not a valid SMS notification");
            return;
        }

        Log.e(TAG, "✅ Valid SMS notification - extracting content");

        // Extract SMS content from notification
        String smsContent = extractSmsContent(sbn);
        if (smsContent != null && !smsContent.isEmpty()) {
            Log.e(TAG, "✅ SMS content extracted: " + smsContent);

            // Extract sender information
            String sender = extractSender(sbn);

            // Extract verification codes
            List<String> verificationCodes = VerificationCodeExtractor.extractVerificationCodes(smsContent);
            String primaryCode = VerificationCodeExtractor.getPrimaryVerificationCode(smsContent);

            if (!verificationCodes.isEmpty()) {
                Log.e(TAG, "✅ Verification codes found: " + verificationCodes.toString());
                Log.e(TAG, "✅ Primary verification code: " + primaryCode);
            } else {
                Log.e(TAG, "⚠️ No verification codes found in SMS");
            }

            // Process message with single streamlined path
            Log.e(TAG, "🔄 Processing SMS message");
            processSmsMessage(smsContent, sender, sbn.getPackageName(), sbn.getPostTime(),
                            verificationCodes, primaryCode);
        } else {
            Log.e(TAG, "❌ No SMS content could be extracted from notification");
        }
    }
    
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        // Optional: Handle notification removal if needed
    }
    
    /**
     * Check if the notification is from an SMS application
     */
    private boolean isSmsNotification(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();

        for (String smsPackage : SMS_PACKAGES) {
            if (smsPackage.equals(packageName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Additional validation for SMS notifications
     * Ensures the notification is actually an SMS and not just from an SMS app
     * Compatible with Android 6.0+ (API 23+)
     */
    private boolean isValidSmsNotification(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();

        if (notification == null) {
            return false;
        }

        // Check if notification has the characteristics of an SMS
        Bundle extras = notification.extras;
        if (extras == null) {
            return false;
        }

        // Look for SMS-specific indicators (handle both String and SpannableString)
        String title = getStringFromExtras(extras, Notification.EXTRA_TITLE);
        String text = getStringFromExtras(extras, Notification.EXTRA_TEXT);
        String bigText = getStringFromExtras(extras, Notification.EXTRA_BIG_TEXT);

        // Must have some text content
        if (TextUtils.isEmpty(text) && TextUtils.isEmpty(bigText)) {
            return false;
        }

        // Filter out non-SMS notifications from SMS apps (like settings, etc.)
        if (title != null && (
            title.toLowerCase().contains("settings") ||
            title.toLowerCase().contains("notification") ||
            title.toLowerCase().contains("permission")
        )) {
            return false;
        }

        return true;
    }
    
    /**
     * Extract SMS content from the notification
     */
    private String extractSmsContent(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();

        if (notification == null) {
            return null;
        }

        // Try to get content from notification extras
        Bundle extras = notification.extras;
        if (extras != null) {
            // Try different text fields that might contain SMS content (handle both String and SpannableString)
            String title = getStringFromExtras(extras, Notification.EXTRA_TITLE);
            String text = getStringFromExtras(extras, Notification.EXTRA_TEXT);
            String bigText = getStringFromExtras(extras, Notification.EXTRA_BIG_TEXT);
            String subText = getStringFromExtras(extras, Notification.EXTRA_SUB_TEXT);

            // Prefer big text if available, otherwise use regular text
            String content = !TextUtils.isEmpty(bigText) ? bigText : text;

            if (!TextUtils.isEmpty(content)) {
                Log.d(TAG, "Extracted SMS - Title: " + title + ", Content: " + content);
                return content.trim();
            }
        }

        return null;
    }

    /**
     * Extract sender information from the notification
     */
    private String extractSender(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();

        if (notification == null) {
            return "Unknown";
        }

        Bundle extras = notification.extras;
        if (extras != null) {
            // Try to get sender from title or sub text (handle both String and SpannableString)
            String title = getStringFromExtras(extras, Notification.EXTRA_TITLE);
            String subText = getStringFromExtras(extras, Notification.EXTRA_SUB_TEXT);

            // Title often contains sender name or phone number
            if (!TextUtils.isEmpty(title)) {
                return title.trim();
            }

            // Fallback to sub text
            if (!TextUtils.isEmpty(subText)) {
                return subText.trim();
            }
        }

        // Fallback to package name
        return sbn.getPackageName();
    }
    
    /**
     * Broadcast SMS content to MainActivity for UI update
     */
    private void broadcastSmsContent(String content, String sender, List<String> verificationCodes,
                                   String primaryCode, String packageName, long timestamp) {
        // Create intent to broadcast SMS content
        Intent intent = new Intent("com.cht.smsforward.SMS_RECEIVED");
        intent.putExtra("sms_content", content);
        intent.putExtra("sender", sender);
        intent.putExtra("package_name", packageName);
        intent.putExtra("timestamp", timestamp);

        // Add verification code information
        if (!verificationCodes.isEmpty()) {
            intent.putStringArrayListExtra("verification_codes", new ArrayList<>(verificationCodes));
            intent.putExtra("primary_verification_code", primaryCode);
        }

        // Send local broadcast only (simplified approach)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        Log.d(TAG, "SMS content broadcasted to MainActivity - Sender: " + sender +
              ", Verification codes: " + verificationCodes.size());
    }

    /**
     * Broadcast SMS status update to MainActivity for UI refresh
     */
    private void broadcastStatusUpdate(SmsMessage smsMessage) {
        // Create intent to broadcast status update
        Intent intent = new Intent("com.cht.smsforward.SMS_STATUS_UPDATE");
        intent.putExtra("sender", smsMessage.getSender());
        intent.putExtra("content", smsMessage.getContent());
        intent.putExtra("timestamp", smsMessage.getTimestamp());
        intent.putExtra("forward_status", smsMessage.getForwardStatus().name());
        intent.putExtra("forward_error", smsMessage.getForwardError());

        // Send local broadcast
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        Log.d(TAG, "SMS status update broadcasted - Sender: " + smsMessage.getSender() +
              ", Forward Status: " + smsMessage.getForwardStatus() +
              ", Forward Error: " + smsMessage.getForwardError());
    }



    /**
     * Safely extract string from Bundle extras, handling both String and SpannableString
     */
    private String getStringFromExtras(Bundle extras, String key) {
        try {
            Object value = extras.get(key);
            if (value == null) {
                return null;
            }

            // Handle both String and CharSequence (including SpannableString)
            if (value instanceof String) {
                return (String) value;
            } else if (value instanceof CharSequence) {
                return value.toString();
            } else {
                Log.d(TAG, "Unexpected type for key " + key + ": " + value.getClass().getSimpleName());
                return value.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting string for key " + key, e);
            return null;
        }
    }





    /**
     * Process SMS message with optimized asynchronous processing
     */
    private void processSmsMessage(String content, String sender, String packageName,
                                 long timestamp, List<String> verificationCodes, String primaryCode) {

        try {
            // Create SMS message object first
            List<String> codes = verificationCodes != null ? verificationCodes : new ArrayList<>();
            SmsMessage smsMessage = new SmsMessage(content, sender, packageName, timestamp, codes, primaryCode);

            // 同步保存到数据库以确保UI能立即看到新消息
            smsDataManager.addSmsMessage(smsMessage);
            Log.d(TAG, "SMS message processed and saved");

            // 验证保存是否成功
            List<SmsMessage> allMessages = smsDataManager.loadSmsMessages();
            Log.d(TAG, "Verification: Total messages in database after save: " + allMessages.size());

            // 立即广播UI更新（现在数据库已经有数据了）
            broadcastSmsContent(content, sender, verificationCodes, primaryCode, packageName, timestamp);

            // 异步处理转发以避免阻塞
            if (primaryCode != null) {
                // 统一的转发处理
                forwardVerificationCode(smsMessage, primaryCode, content, sender);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing SMS message", e);

            // Fallback: queue the message for later processing
            messageQueue.queueMessage(content, sender, packageName, timestamp, verificationCodes, primaryCode);
            Log.d(TAG, "Message queued for later processing due to error");
        }
    }

    /**
     * 统一的验证码转发处理方法
     * 消除邮件和Server酱发送逻辑的重复代码
     */
    private void forwardVerificationCode(SmsMessage smsMessage, String primaryCode, String content, String sender) {
        UnifiedSettingsManager settingsManager = new UnifiedSettingsManager(this);

        // 邮件转发
        new Thread(() -> {
            forwardToEmailService(smsMessage, primaryCode, content, sender, settingsManager.loadEmailConfig());
        }, "Email-Sending-" + System.currentTimeMillis()).start();

        // Server酱转发
        new Thread(() -> {
            forwardToServerChanService(smsMessage, primaryCode, content, sender, settingsManager.loadServerChanConfig());
        }, "ServerChan-Sending-" + System.currentTimeMillis()).start();
    }

    /**
     * 邮件转发处理方法
     */
    private void forwardToEmailService(SmsMessage smsMessage, String primaryCode, String content, String sender, EmailConfig config) {
        try {
            Log.d(TAG, "Attempting to send verification code via Email: " + primaryCode);

            // 检查邮件服务是否启用
            if (!config.isEnabled() || !config.isValid()) {
                Log.d(TAG, "Email forwarding is disabled or invalid, skipping send");
                smsMessage.setEmailFailed("disabled");
                smsDataManager.updateSmsMessage(smsMessage);
                return;
            }

            // 设置发送中状态
            smsMessage.setEmailSending();
            smsDataManager.updateSmsMessage(smsMessage);
            broadcastStatusUpdate(smsMessage);

            // 执行邮件发送
            emailSender.sendVerificationCodeEmail(primaryCode, content, sender, new EmailSender.EmailSendCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Verification code email sent successfully");
                    smsMessage.setEmailSent();
                    smsDataManager.updateSmsMessage(smsMessage);
                    broadcastStatusUpdate(smsMessage);
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Failed to send verification code email: " + error);
                    smsMessage.setEmailFailed(error);
                    smsDataManager.updateSmsMessage(smsMessage);
                    broadcastStatusUpdate(smsMessage);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error sending email", e);
            smsMessage.setEmailFailed(e.getMessage());
            smsDataManager.updateSmsMessage(smsMessage);
            broadcastStatusUpdate(smsMessage);
        }
    }

    /**
     * Server酱转发处理方法
     */
    private void forwardToServerChanService(SmsMessage smsMessage, String primaryCode, String content, String sender, ServerChanConfig config) {
        try {
            Log.d(TAG, "Attempting to send verification code via Server酱: " + primaryCode);

            // 检查Server酱服务是否启用
            if (!config.isEnabled() || !config.isValid()) {
                Log.d(TAG, "Server酱 forwarding is disabled or invalid, skipping send");
                smsMessage.setServerChanFailed("disabled");
                smsDataManager.updateSmsMessage(smsMessage);
                return;
            }

            // 设置发送中状态
            smsMessage.setServerChanSending();
            smsDataManager.updateSmsMessage(smsMessage);
            broadcastStatusUpdate(smsMessage);

            // 执行Server酱发送
            serverChanSender.sendVerificationCodeMessage(primaryCode, content, sender, new ServerChanSender.ServerChanSendCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Verification code sent to Server酱 successfully");
                    smsMessage.setServerChanSent();
                    smsDataManager.updateSmsMessage(smsMessage);
                    broadcastStatusUpdate(smsMessage);
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Failed to send verification code to Server酱: " + error);
                    smsMessage.setServerChanFailed(error);
                    smsDataManager.updateSmsMessage(smsMessage);
                    broadcastStatusUpdate(smsMessage);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error sending to Server酱", e);
            smsMessage.setServerChanFailed(e.getMessage());
            smsDataManager.updateSmsMessage(smsMessage);
            broadcastStatusUpdate(smsMessage);
        }
    }
}
