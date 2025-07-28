package com.cht.smsforward.sender;

import com.cht.smsforward.config.ForwardingConfig;
import com.cht.smsforward.config.UnifiedSettingsManager;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Base class for all message senders (Email, ServerChan, etc.)
 * Provides common functionality for async sending, configuration validation, and error handling
 */
public abstract class MessageSender<T extends ForwardingConfig> {
    protected final String TAG;
    protected final Context context;
    protected final UnifiedSettingsManager settingsManager;
    
    /**
     * Common callback interface for all message sending operations
     */
    public interface SendCallback {
        void onSuccess();
        void onFailure(String error);
    }
    
    public MessageSender(Context context, String tag) {
        this.context = context.getApplicationContext();
        this.TAG = tag;
        this.settingsManager = new UnifiedSettingsManager(context);
    }
    
    /**
     * Send verification code message asynchronously
     */
    public void sendVerificationCodeMessage(String verificationCode, String smsContent, String sender, SendCallback callback) {
        try {
            T config = loadConfig();
            
            if (!config.isValid() || !config.isEnabled()) {
                String error = getServiceName() + " configuration is invalid or disabled";
                Log.w(TAG, error + " - Config: " + config.toString());
                if (callback != null) {
                    callback.onFailure(error);
                }
                return;
            }
            
            Log.d(TAG, "Sending verification code via " + getServiceName() + " - Code: " + verificationCode + ", Sender: " + sender);
            new SendVerificationTask(config, verificationCode, smsContent, sender, callback).execute();
        } catch (Exception e) {
            String error = "Failed to initiate " + getServiceName() + " sending: " + e.getMessage();
            Log.e(TAG, error, e);
            if (callback != null) {
                callback.onFailure(error);
            }
        }
    }
    
    /**
     * Test configuration and send a test message
     */
    public void sendTestMessage(T config, SendCallback callback) {
        if (!config.isValid()) {
            Log.e(TAG, getServiceName() + " configuration is invalid for testing");
            if (callback != null) {
                callback.onFailure(getServiceName() + " configuration is invalid");
            }
            return;
        }
        
        new SendTestTask(config, callback).execute();
    }
    
    /**
     * AsyncTask for sending verification code messages
     */
    private class SendVerificationTask extends AsyncTask<Void, Void, String> {
        private final T config;
        private final String verificationCode;
        private final String smsContent;
        private final String sender;
        private final SendCallback callback;
        
        public SendVerificationTask(T config, String verificationCode, String smsContent, String sender, SendCallback callback) {
            this.config = config;
            this.verificationCode = verificationCode;
            this.smsContent = smsContent;
            this.sender = sender;
            this.callback = callback;
        }
        
        @Override
        protected String doInBackground(Void... voids) {
            try {
                return sendVerificationMessage(config, verificationCode, smsContent, sender);
            } catch (Exception e) {
                String error = "Failed to send verification message via " + getServiceName() + ": " + e.getMessage();
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
     * AsyncTask for sending test messages
     */
    private class SendTestTask extends AsyncTask<Void, Void, String> {
        private final T config;
        private final SendCallback callback;
        
        public SendTestTask(T config, SendCallback callback) {
            this.config = config;
            this.callback = callback;
        }
        
        @Override
        protected String doInBackground(Void... voids) {
            try {
                return sendTestMessage(config);
            } catch (Exception e) {
                String error = "Failed to send test message via " + getServiceName() + ": " + e.getMessage();
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
    
    // Abstract methods that subclasses must implement
    
    /**
     * Load the configuration for this sender type
     */
    protected abstract T loadConfig();
    
    /**
     * Get the service name for logging and error messages
     */
    protected abstract String getServiceName();
    
    /**
     * Send a verification message with the given parameters
     * @return null on success, error message on failure
     */
    protected abstract String sendVerificationMessage(T config, String verificationCode, String smsContent, String sender);
    
    /**
     * Send a test message with the given configuration
     * @return null on success, error message on failure
     */
    protected abstract String sendTestMessage(T config);
}
