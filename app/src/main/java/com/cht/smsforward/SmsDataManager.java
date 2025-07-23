package com.cht.smsforward;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理SMS消息的持久化存储
 */
public class SmsDataManager {
    
    private static final String TAG = "SmsDataManager";
    private static final String PREFS_NAME = "sms_data";
    private static final String KEY_SMS_MESSAGES = "sms_messages";
    private static final int MAX_STORED_MESSAGES = 100; // 最多存储100条消息
    
    private SharedPreferences prefs;
    private Gson gson;
    
    public SmsDataManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Create Gson with custom EmailForwardStatus serializer/deserializer
        gson = new GsonBuilder()
                .registerTypeAdapter(EmailForwardStatus.class, new EmailForwardStatusAdapter())
                .create();
    }

    /**
     * Custom Gson adapter for EmailForwardStatus enum
     */
    private static class EmailForwardStatusAdapter implements JsonSerializer<EmailForwardStatus>, JsonDeserializer<EmailForwardStatus> {
        @Override
        public JsonElement serialize(EmailForwardStatus src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getValue());
        }

        @Override
        public EmailForwardStatus deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return EmailForwardStatus.fromValue(json.getAsString());
        }
    }
    
    /**
     * 保存SMS消息列表
     */
    public void saveSmsMessages(List<SmsMessage> messages) {
        try {
            // 按时间戳降序排序（最新的在前面）
            List<SmsMessage> sortedMessages = new ArrayList<>(messages);
            sortedMessages.sort((msg1, msg2) -> Long.compare(msg2.getTimestamp(), msg1.getTimestamp()));

            // 限制存储数量，只保留最新的消息
            List<SmsMessage> messagesToSave = sortedMessages;
            if (sortedMessages.size() > MAX_STORED_MESSAGES) {
                messagesToSave = sortedMessages.subList(0, MAX_STORED_MESSAGES);
            }

            String json = gson.toJson(messagesToSave);
            prefs.edit().putString(KEY_SMS_MESSAGES, json).apply();
            Log.d(TAG, "Saved " + messagesToSave.size() + " SMS messages (sorted by timestamp)");
        } catch (Exception e) {
            Log.e(TAG, "Error saving SMS messages", e);
        }
    }
    
    /**
     * 加载SMS消息列表
     */
    public List<SmsMessage> loadSmsMessages() {
        try {
            String json = prefs.getString(KEY_SMS_MESSAGES, null);
            if (json != null) {
                Type listType = new TypeToken<List<SmsMessage>>(){}.getType();
                List<SmsMessage> messages = gson.fromJson(json, listType);

                // Post-process messages to ensure proper initialization
                if (messages != null) {
                    for (SmsMessage message : messages) {
                        // Ensure email forward status is properly initialized
                        if (message.getEmailForwardStatus() == null) {
                            if (message.hasVerificationCodes()) {
                                message.setEmailForwardStatus(EmailForwardStatus.NOT_SENT);
                            } else {
                                message.setEmailForwardStatus(EmailForwardStatus.DISABLED);
                            }
                        }
                    }
                    Log.d(TAG, "Loaded " + messages.size() + " SMS messages");
                    return messages;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading SMS messages", e);
        }
        return new ArrayList<>();
    }
    
    /**
     * 添加新的SMS消息
     */
    public void addSmsMessage(SmsMessage newMessage) {
        List<SmsMessage> messages = loadSmsMessages();

        // 找到正确的插入位置（按时间戳降序）
        int insertIndex = 0;
        for (int i = 0; i < messages.size(); i++) {
            if (newMessage.getTimestamp() > messages.get(i).getTimestamp()) {
                insertIndex = i;
                break;
            }
            insertIndex = i + 1;
        }

        messages.add(insertIndex, newMessage);
        saveSmsMessages(messages);
    }
    
    /**
     * 更新现有的SMS消息
     */
    public void updateSmsMessage(SmsMessage updatedMessage) {
        List<SmsMessage> messages = loadSmsMessages();

        // 查找并更新匹配的消息（基于时间戳和发送者）
        for (int i = 0; i < messages.size(); i++) {
            SmsMessage message = messages.get(i);
            if (message.getTimestamp() == updatedMessage.getTimestamp() &&
                message.getSender().equals(updatedMessage.getSender()) &&
                message.getContent().equals(updatedMessage.getContent())) {
                messages.set(i, updatedMessage);
                saveSmsMessages(messages);
                Log.d(TAG, "Updated SMS message with email status: " + updatedMessage.getEmailForwardStatus());
                return;
            }
        }

        Log.w(TAG, "Could not find SMS message to update");
    }

    /**
     * 清除所有SMS消息
     */
    public void clearSmsMessages() {
        prefs.edit().remove(KEY_SMS_MESSAGES).apply();
        Log.d(TAG, "Cleared all SMS messages");
    }
}
