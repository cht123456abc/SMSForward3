package com.cht.smsforward;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Server酱 sender utility for sending verification codes via Server酱 API
 */
public class ServerChanSender {
    private static final String TAG = "ServerChanSender";
    
    private Context context;
    private ServerChanSettingsManager settingsManager;
    
    // HTTP connection timeout settings
    private static final int CONNECTION_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 15000; // 15 seconds
    
    public ServerChanSender(Context context) {
        this.context = context;
        this.settingsManager = new ServerChanSettingsManager(context);
    }
    
    /**
     * Callback interface for Server酱 send operations
     */
    public interface ServerChanSendCallback {
        void onSuccess();
        void onFailure(String error);
    }
    
    /**
     * Send verification code message to Server酱 asynchronously
     */
    public void sendVerificationCodeMessage(String verificationCode, String smsContent, String sender, ServerChanSendCallback callback) {
        try {
            ServerChanConfig config = settingsManager.loadServerChanConfig();
            
            if (!config.isValid() || !config.isEnabled()) {
                String error = "Server酱 configuration is invalid or disabled";
                Log.w(TAG, error + " - Config: " + config.toString());
                if (callback != null) {
                    callback.onFailure(error);
                }
                return;
            }
            
            Log.d(TAG, "Sending verification code to Server酱 - Code: " + verificationCode + ", Sender: " + sender);
            new SendMessageTask(config, verificationCode, smsContent, sender, callback).execute();
        } catch (Exception e) {
            String error = "Failed to initiate Server酱 sending: " + e.getMessage();
            Log.e(TAG, error, e);
            if (callback != null) {
                callback.onFailure(error);
            }
        }
    }
    
    /**
     * Test Server酱 connection and send a test message
     */
    public void sendTestMessage(ServerChanConfig config, ServerChanSendCallback callback) {
        if (!config.isValid()) {
            Log.e(TAG, "Server酱 configuration is invalid for testing");
            if (callback != null) {
                callback.onFailure("Server酱 configuration is invalid");
            }
            return;
        }
        
        new SendTestMessageTask(config, callback).execute();
    }
    
    /**
     * AsyncTask for sending verification code messages to Server酱
     */
    private static class SendMessageTask extends AsyncTask<Void, Void, String> {
        private final ServerChanConfig config;
        private final String verificationCode;
        private final String smsContent;
        private final String sender;
        private final ServerChanSendCallback callback;
        
        public SendMessageTask(ServerChanConfig config, String verificationCode, String smsContent, String sender, ServerChanSendCallback callback) {
            this.config = config;
            this.verificationCode = verificationCode;
            this.smsContent = smsContent;
            this.sender = sender;
            this.callback = callback;
        }
        
        @Override
        protected String doInBackground(Void... voids) {
            try {
                // Create message title and content
                String title = "SMS验证码 - " + verificationCode;
                String content = createVerificationMessageContent(verificationCode, smsContent, sender);
                
                // Send message to Server酱
                return sendToServerChan(config.getApiUrl(), title, content);
                
            } catch (Exception e) {
                String error = "Failed to send verification message to Server酱: " + e.getMessage();
                Log.e(TAG, error, e);
                return error;
            }
        }
        
        @Override
        protected void onPostExecute(String error) {
            if (callback != null) {
                if (error == null) {
                    callback.onSuccess();
                } else {
                    callback.onFailure(error);
                }
            }
        }
    }
    
    /**
     * AsyncTask for sending test messages to Server酱
     */
    private static class SendTestMessageTask extends AsyncTask<Void, Void, String> {
        private final ServerChanConfig config;
        private final ServerChanSendCallback callback;
        
        public SendTestMessageTask(ServerChanConfig config, ServerChanSendCallback callback) {
            this.config = config;
            this.callback = callback;
        }
        
        @Override
        protected String doInBackground(Void... voids) {
            try {
                String title = "SMS Forward - 测试消息";
                String content = "这是来自 SMS Forward 应用的测试消息。\n\n" +
                        "如果您收到此消息，说明您的 Server酱 配置正常工作。\n\n" +
                        "发送时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                
                return sendToServerChan(config.getApiUrl(), title, content);
                
            } catch (Exception e) {
                String error = "Failed to send test message to Server酱: " + e.getMessage();
                Log.e(TAG, error, e);
                return error;
            }
        }
        
        @Override
        protected void onPostExecute(String error) {
            if (callback != null) {
                if (error == null) {
                    callback.onSuccess();
                } else {
                    callback.onFailure(error);
                }
            }
        }
    }
    
    /**
     * Create formatted message content for verification codes
     */
    private static String createVerificationMessageContent(String verificationCode, String smsContent, String sender) {
        StringBuilder content = new StringBuilder();
        content.append("📱 收到新的短信验证码\n\n");
        content.append("🔢 验证码: ").append(verificationCode).append("\n");
        content.append("📞 发送方: ").append(sender != null ? sender : "未知").append("\n");
        content.append("⏰ 时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n\n");
        content.append("📄 完整内容:\n").append(smsContent);
        
        return content.toString();
    }
    
    /**
     * Send HTTP POST request to Server酱 API
     */
    private static String sendToServerChan(String apiUrl, String title, String content) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // Set up connection
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setDoOutput(true);
            
            // Prepare POST data
            String postData = "title=" + URLEncoder.encode(title, "UTF-8") +
                            "&desp=" + URLEncoder.encode(content, "UTF-8");
            
            // Send request
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.writeBytes(postData);
                wr.flush();
            }
            
            // Read response
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Server酱 API response code: " + responseCode);
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    responseCode >= 200 && responseCode < 300 ? connection.getInputStream() : connection.getErrorStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            Log.d(TAG, "Server酱 API response: " + response.toString());
            
            if (responseCode >= 200 && responseCode < 300) {
                Log.d(TAG, "Message sent to Server酱 successfully");
                return null; // Success
            } else {
                return "Server酱 API error (HTTP " + responseCode + "): " + response.toString();
            }
            
        } finally {
            connection.disconnect();
        }
    }
}
