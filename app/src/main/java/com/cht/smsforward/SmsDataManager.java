package com.cht.smsforward;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
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
        gson = new Gson();
    }
    
    /**
     * 保存SMS消息列表
     */
    public void saveSmsMessages(List<SmsMessage> messages) {
        try {
            // 限制存储数量，只保留最新的消息
            List<SmsMessage> messagesToSave = messages;
            if (messages.size() > MAX_STORED_MESSAGES) {
                messagesToSave = messages.subList(0, MAX_STORED_MESSAGES);
            }
            
            String json = gson.toJson(messagesToSave);
            prefs.edit().putString(KEY_SMS_MESSAGES, json).apply();
            Log.d(TAG, "Saved " + messagesToSave.size() + " SMS messages");
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
                Log.d(TAG, "Loaded " + messages.size() + " SMS messages");
                return messages != null ? messages : new ArrayList<>();
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
        messages.add(0, newMessage); // 添加到列表开头
        saveSmsMessages(messages);
    }
    
    /**
     * 清除所有SMS消息
     */
    public void clearSmsMessages() {
        prefs.edit().remove(KEY_SMS_MESSAGES).apply();
        Log.d(TAG, "Cleared all SMS messages");
    }
}
