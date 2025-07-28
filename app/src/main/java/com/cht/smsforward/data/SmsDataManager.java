package com.cht.smsforward.data;

import com.cht.smsforward.model.ForwardStatus;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 管理SMS消息的持久化存储 - 优化版本
 * 特性：
 * - 内存缓存减少I/O操作
 * - 异步数据库操作
 * - 批量更新优化
 * - 重复检测优化
 */
public class SmsDataManager {

    private static final String TAG = "SmsDataManager";
    private static final String PREFS_NAME = "sms_data";
    private static final String KEY_SMS_MESSAGES = "sms_messages";
    private static final int MAX_STORED_MESSAGES = 100; // 最多存储100条消息

    private SharedPreferences prefs;
    private Gson gson;

    // 性能优化：内存缓存
    private List<SmsMessage> cachedMessages;
    private final AtomicBoolean cacheLoaded = new AtomicBoolean(false);
    private final Object cacheLock = new Object();

    // 异步操作
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    // 重复检测优化：使用哈希表快速查找
    private final ConcurrentHashMap<String, Long> messageHashes = new ConcurrentHashMap<>();

    public SmsDataManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Create Gson with custom ForwardStatus serializer/deserializer
        gson = new GsonBuilder()
                .registerTypeAdapter(ForwardStatus.class, new ForwardStatusAdapter())
                .create();

        // 初始化后台线程用于异步操作
        backgroundThread = new HandlerThread("SmsDataManager-Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        // 预加载缓存（异步）
        backgroundHandler.post(this::preloadCache);
    }

    /**
     * 预加载缓存以提高性能
     */
    private void preloadCache() {
        synchronized (cacheLock) {
            if (!cacheLoaded.get()) {
                cachedMessages = loadSmsMessagesInternal();
                buildMessageHashIndex();
                cacheLoaded.set(true);
                Log.d(TAG, "Cache preloaded with " + cachedMessages.size() + " messages");
            }
        }
    }

    /**
     * 强制重新加载缓存（用于调试）
     */
    public void forceReloadCache() {
        synchronized (cacheLock) {
            cachedMessages = loadSmsMessagesInternal();
            buildMessageHashIndex();
            cacheLoaded.set(true);
            Log.d(TAG, "Cache force reloaded with " + cachedMessages.size() + " messages");
        }
    }

    /**
     * 构建消息哈希索引用于快速重复检测
     */
    private void buildMessageHashIndex() {
        messageHashes.clear();
        for (SmsMessage message : cachedMessages) {
            String hash = createMessageHash(message);
            messageHashes.put(hash, message.getTimestamp());
        }
    }

    /**
     * 创建消息哈希用于重复检测
     */
    private String createMessageHash(SmsMessage message) {
        return message.getContent() + "|" + message.getSender() + "|" + (message.getTimestamp() / 1000);
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
        }
    }

    /**
     * Custom Gson adapter for unified ForwardStatus enum
     */
    private static class ForwardStatusAdapter implements JsonSerializer<ForwardStatus>, JsonDeserializer<ForwardStatus> {
        @Override
        public JsonElement serialize(ForwardStatus src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getValue());
        }

        @Override
        public ForwardStatus deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return ForwardStatus.fromValue(json.getAsString());
        }
    }


    
    /**
     * 保存SMS消息列表（优化版本 - 异步操作）
     */
    public void saveSmsMessages(List<SmsMessage> messages) {
        // 异步保存以避免阻塞主线程
        backgroundHandler.post(() -> saveSmsMessagesInternal(messages));
    }

    /**
     * 内部同步保存方法
     */
    private void saveSmsMessagesInternal(List<SmsMessage> messages) {
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

            // 更新缓存
            synchronized (cacheLock) {
                cachedMessages = new ArrayList<>(messagesToSave);
                buildMessageHashIndex();
            }

            Log.d(TAG, "Saved " + messagesToSave.size() + " SMS messages (sorted by timestamp)");
        } catch (Exception e) {
            Log.e(TAG, "Error saving SMS messages", e);
        }
    }
    
    /**
     * 加载SMS消息列表（优化版本 - 使用缓存）
     */
    public List<SmsMessage> loadSmsMessages() {
        synchronized (cacheLock) {
            if (cacheLoaded.get() && cachedMessages != null) {
                // 返回缓存副本以避免并发修改
                return new ArrayList<>(cachedMessages);
            }
        }

        // 缓存未加载，同步加载
        return loadSmsMessagesInternal();
    }

    /**
     * 内部加载方法（从存储读取）
     */
    private List<SmsMessage> loadSmsMessagesInternal() {
        try {
            String json = prefs.getString(KEY_SMS_MESSAGES, null);
            if (json != null) {
                Type listType = new TypeToken<List<SmsMessage>>(){}.getType();
                List<SmsMessage> messages = gson.fromJson(json, listType);

                // Messages are now properly initialized in their constructors
                if (messages != null) {
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
     * 添加新的SMS消息（优化版本 - 快速重复检测，同步保存以确保UI及时更新）
     */
    public void addSmsMessage(SmsMessage newMessage) {
        synchronized (cacheLock) {
            // 快速重复检测使用哈希表
            String messageHash = createMessageHash(newMessage);
            Long existingTimestamp = messageHashes.get(messageHash);

            if (existingTimestamp != null) {
                // 检查时间差是否在容忍范围内（1秒）
                long timeDiff = Math.abs(existingTimestamp - newMessage.getTimestamp());
                if (timeDiff <= 1000) {
                    Log.d(TAG, "Duplicate message detected (fast check) - skipping: " + newMessage.getSender());
                    return;
                }
            }

            // 确保缓存已加载
            if (!cacheLoaded.get()) {
                cachedMessages = loadSmsMessagesInternal();
                buildMessageHashIndex();
                cacheLoaded.set(true);
            }

            // 获取当前消息列表（使用缓存）
            List<SmsMessage> messages = new ArrayList<>(cachedMessages);

            // 找到正确的插入位置（按时间戳降序）
            int insertIndex = 0;
            for (int i = 0; i < messages.size(); i++) {
                if (newMessage.getTimestamp() > messages.get(i).getTimestamp()) {
                    insertIndex = i;
                    break;
                }
                insertIndex = i + 1;
            }

            // 添加到列表和哈希索引
            messages.add(insertIndex, newMessage);
            messageHashes.put(messageHash, newMessage.getTimestamp());

            // 立即更新缓存
            cachedMessages = new ArrayList<>(messages);

            // 同步保存到存储
            saveSmsMessagesInternal(messages);
            Log.d(TAG, "New SMS message added - Sender: " + newMessage.getSender() + " at " + newMessage.getFormattedTimestamp() + ", total messages: " + messages.size());
        }
    }

    /**
     * 检查两个消息是否重复
     */
    private boolean isDuplicateMessage(SmsMessage existing, SmsMessage newMessage) {
        // 基于内容、发送者和时间戳进行重复检测
        // 允许时间戳有小的差异（1秒内）以处理系统时间差异
        long timeDiff = Math.abs(existing.getTimestamp() - newMessage.getTimestamp());
        boolean sameContent = existing.getContent().equals(newMessage.getContent());
        boolean sameSender = existing.getSender().equals(newMessage.getSender());
        boolean similarTime = timeDiff <= 1000; // 1秒内认为是相同时间

        return sameContent && sameSender && similarTime;
    }
    
    /**
     * 更新现有的SMS消息（优化版本 - 同步保存以确保状态更新的实时性）
     */
    public void updateSmsMessage(SmsMessage updatedMessage) {
        synchronized (cacheLock) {
            if (cacheLoaded.get() && cachedMessages != null) {
                // 在缓存中查找并更新
                for (int i = 0; i < cachedMessages.size(); i++) {
                    SmsMessage message = cachedMessages.get(i);
                    if (message.getTimestamp() == updatedMessage.getTimestamp() &&
                        message.getSender().equals(updatedMessage.getSender()) &&
                        message.getContent().equals(updatedMessage.getContent())) {

                        cachedMessages.set(i, updatedMessage);

                        // 同步保存到存储以确保状态更新的实时性
                        saveSmsMessagesInternal(new ArrayList<>(cachedMessages));

                        Log.d(TAG, "Updated SMS message with forward status: " + updatedMessage.getForwardStatus());
                        return;
                    }
                }
            }
        }

        // 回退到原始方法（如果缓存未加载）
        List<SmsMessage> messages = loadSmsMessages();
        for (int i = 0; i < messages.size(); i++) {
            SmsMessage message = messages.get(i);
            if (message.getTimestamp() == updatedMessage.getTimestamp() &&
                message.getSender().equals(updatedMessage.getSender()) &&
                message.getContent().equals(updatedMessage.getContent())) {
                messages.set(i, updatedMessage);
                // 同步保存以确保状态更新的实时性
                saveSmsMessagesInternal(messages);
                Log.d(TAG, "Updated SMS message with forward status: " + updatedMessage.getForwardStatus());
                return;
            }
        }

        Log.w(TAG, "Could not find SMS message to update");
    }

    /**
     * 清除所有SMS消息（优化版本）
     */
    public void clearSmsMessages() {
        // 清除缓存
        synchronized (cacheLock) {
            if (cachedMessages != null) {
                cachedMessages.clear();
            }
            messageHashes.clear();
        }

        // 异步清除存储
        backgroundHandler.post(() -> {
            prefs.edit().remove(KEY_SMS_MESSAGES).apply();
            Log.d(TAG, "Cleared all SMS messages");
        });
    }
}
