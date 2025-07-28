package com.cht.smsforward.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistent message queue for storing SMS messages when the app is backgrounded
 * Ensures no messages are lost during background processing
 */
public class MessageQueue {
    
    private static final String TAG = "MessageQueue";
    private static final String PREFS_NAME = "message_queue";
    private static final String KEY_QUEUED_MESSAGES = "queued_messages";
    private static final int MAX_QUEUE_SIZE = 50; // Maximum queued messages
    
    private SharedPreferences prefs;
    private Gson gson;
    
    public MessageQueue(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new GsonBuilder()
                .create();
    }
    
    /**
     * Add a message to the queue
     */
    public void queueMessage(String content, String sender, String packageName, long timestamp, 
                           List<String> verificationCodes, String primaryCode) {
        try {
            List<QueuedMessage> queuedMessages = getQueuedMessages();
            
            // Create new queued message
            QueuedMessage newMessage = new QueuedMessage(
                content, sender, packageName, timestamp, verificationCodes, primaryCode
            );
            
            // Add to queue
            queuedMessages.add(newMessage);
            
            // Limit queue size
            if (queuedMessages.size() > MAX_QUEUE_SIZE) {
                queuedMessages = queuedMessages.subList(
                    queuedMessages.size() - MAX_QUEUE_SIZE, 
                    queuedMessages.size()
                );
            }
            
            // Save updated queue
            saveQueuedMessages(queuedMessages);
            
            Log.d(TAG, "Message queued - Total queued: " + queuedMessages.size());
            
        } catch (Exception e) {
            Log.e(TAG, "Error queuing message", e);
        }
    }
    
    /**
     * Get all queued messages
     */
    public List<QueuedMessage> getQueuedMessages() {
        try {
            String json = prefs.getString(KEY_QUEUED_MESSAGES, null);
            if (json != null) {
                Type listType = new TypeToken<List<QueuedMessage>>(){}.getType();
                List<QueuedMessage> messages = gson.fromJson(json, listType);
                return messages != null ? messages : new ArrayList<>();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading queued messages", e);
        }
        return new ArrayList<>();
    }
    
    /**
     * Save queued messages to persistent storage
     */
    private void saveQueuedMessages(List<QueuedMessage> messages) {
        try {
            String json = gson.toJson(messages);
            prefs.edit().putString(KEY_QUEUED_MESSAGES, json).apply();
            Log.d(TAG, "Saved " + messages.size() + " queued messages");
        } catch (Exception e) {
            Log.e(TAG, "Error saving queued messages", e);
        }
    }
    
    /**
     * Clear all queued messages
     */
    public void clearQueue() {
        prefs.edit().remove(KEY_QUEUED_MESSAGES).apply();
        Log.d(TAG, "Message queue cleared");
    }
    
    /**
     * Get queue size
     */
    public int getQueueSize() {
        return getQueuedMessages().size();
    }
    
    /**
     * Check if queue is empty
     */
    public boolean isEmpty() {
        return getQueueSize() == 0;
    }
    

}

/**
 * Represents a queued SMS message
 */
class QueuedMessage {
    private String content;
    private String sender;
    private String packageName;
    private long timestamp;
    private List<String> verificationCodes;
    private String primaryCode;
    
    public QueuedMessage(String content, String sender, String packageName, long timestamp,
                        List<String> verificationCodes, String primaryCode) {
        this.content = content;
        this.sender = sender;
        this.packageName = packageName;
        this.timestamp = timestamp;
        this.verificationCodes = verificationCodes != null ? verificationCodes : new ArrayList<>();
        this.primaryCode = primaryCode;
    }
    
    // Getters
    public String getContent() { return content; }
    public String getSender() { return sender; }
    public String getPackageName() { return packageName; }
    public long getTimestamp() { return timestamp; }
    public List<String> getVerificationCodes() { return verificationCodes; }
    public String getPrimaryCode() { return primaryCode; }
    
    /**
     * Convert to SmsMessage object
     */
    public SmsMessage toSmsMessage() {
        return new SmsMessage(content, sender, packageName, timestamp, verificationCodes, primaryCode);
    }
}
