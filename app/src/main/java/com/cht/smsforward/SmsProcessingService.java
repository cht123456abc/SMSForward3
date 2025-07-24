package com.cht.smsforward;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Background service that handles SMS processing and email forwarding
 * when the app is backgrounded, ensuring reliable message processing
 * regardless of app state.
 */
public class SmsProcessingService extends Service {
    
    private static final String TAG = "SmsProcessingService";
    private static final String SMS_RECEIVED_ACTION = "com.cht.smsforward.SMS_RECEIVED";
    
    private SmsBroadcastReceiver smsBroadcastReceiver;
    private SmsDataManager smsDataManager;
    private EmailSender emailSender;
    private ExecutorService executorService;
    private MessageQueue messageQueue;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SMS Processing Service created");
        
        // Initialize components
        smsDataManager = new SmsDataManager(this);
        emailSender = new EmailSender(this);
        executorService = Executors.newSingleThreadExecutor();
        messageQueue = new MessageQueue(this);
        
        // Set up broadcast receiver for SMS notifications
        setupSmsBroadcastReceiver();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SMS Processing Service started");
        
        // Process any queued messages
        processQueuedMessages();
        
        // Return START_STICKY to restart service if killed
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SMS Processing Service destroyed");
        
        // Unregister broadcast receiver
        if (smsBroadcastReceiver != null) {
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(smsBroadcastReceiver);
                unregisterReceiver(smsBroadcastReceiver);
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "Broadcast receiver was not registered");
            }
        }
        
        // Shutdown executor
        if (executorService != null) {
            executorService.shutdown();
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
    
    /**
     * Set up broadcast receiver for SMS notifications
     */
    private void setupSmsBroadcastReceiver() {
        smsBroadcastReceiver = new SmsBroadcastReceiver();
        IntentFilter filter = new IntentFilter(SMS_RECEIVED_ACTION);
        
        // Register with LocalBroadcastManager
        LocalBroadcastManager.getInstance(this).registerReceiver(smsBroadcastReceiver, filter);
        
        // Also register regular receiver as fallback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(smsBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(smsBroadcastReceiver, filter);
        }
        
        Log.d(TAG, "SMS broadcast receiver registered in background service");
    }
    

    
    /**
     * Process queued messages that were received while app was inactive
     */
    private void processQueuedMessages() {
        executorService.execute(() -> {
            List<QueuedMessage> queuedMessages = messageQueue.getQueuedMessages();
            Log.d(TAG, "Processing " + queuedMessages.size() + " queued messages");
            
            for (QueuedMessage queuedMessage : queuedMessages) {
                processMessage(queuedMessage.toSmsMessage());
            }
            
            // Clear processed messages from queue
            messageQueue.clearQueue();
        });
    }
    
    /**
     * Process a single SMS message
     */
    private void processMessage(SmsMessage smsMessage) {
        Log.d(TAG, "Processing SMS message from: " + smsMessage.getSender());
        
        // Save to persistent storage
        smsDataManager.addSmsMessage(smsMessage);
        
        // Send verification code email if available
        if (smsMessage.getPrimaryVerificationCode() != null) {
            sendVerificationCodeEmail(smsMessage);
        }
        
        // Notify MainActivity if it's active
        notifyMainActivity(smsMessage);
    }
    
    /**
     * Send verification code email
     */
    private void sendVerificationCodeEmail(SmsMessage smsMessage) {
        String primaryCode = smsMessage.getPrimaryVerificationCode();
        Log.d(TAG, "Sending verification code email for code: " + primaryCode);
        
        emailSender.sendVerificationCodeEmail(
            primaryCode,
            smsMessage.getContent(),
            smsMessage.getSender(),
            new EmailSender.EmailSendCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Verification code email sent successfully");
                    smsMessage.setEmailForwardStatus(EmailForwardStatus.SUCCESS);
                    smsDataManager.updateSmsMessage(smsMessage);
                }
                
                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Failed to send verification code email: " + error);
                    smsMessage.setEmailForwardStatus(EmailForwardStatus.FAILED);
                    smsDataManager.updateSmsMessage(smsMessage);
                }
            }
        );
    }
    
    /**
     * Notify MainActivity about new message (if it's active)
     */
    private void notifyMainActivity(SmsMessage smsMessage) {
        Intent intent = new Intent("com.cht.smsforward.NEW_MESSAGE_PROCESSED");
        // Instead of passing the whole object, just notify that a new message was processed
        // MainActivity will reload all messages from storage
        intent.putExtra("message_timestamp", smsMessage.getTimestamp());
        intent.putExtra("sender", smsMessage.getSender());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    
    /**
     * Broadcast receiver for SMS notifications
     */
    private class SmsBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "=== BACKGROUND SERVICE BROADCAST RECEIVED ===");
            
            if (SMS_RECEIVED_ACTION.equals(intent.getAction())) {
                Log.d(TAG, "SMS broadcast received in background service - processing");
                
                // Extract SMS data from intent
                String content = intent.getStringExtra("sms_content");
                String sender = intent.getStringExtra("sender");
                String packageName = intent.getStringExtra("package_name");
                long timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());
                
                ArrayList<String> verificationCodes = intent.getStringArrayListExtra("verification_codes");
                String primaryCode = intent.getStringExtra("primary_verification_code");
                
                if (content != null && sender != null) {
                    // Create SMS message object
                    List<String> codes = verificationCodes != null ? verificationCodes : new ArrayList<>();
                    SmsMessage smsMessage = new SmsMessage(content, sender, packageName, timestamp, codes, primaryCode);
                    
                    // Process message in background thread
                    executorService.execute(() -> processMessage(smsMessage));
                } else {
                    Log.w(TAG, "Invalid SMS data received in background service");
                }
            }
        }
    }
}
