package com.cht.smsforward;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Email sender utility for sending verification codes via QQ Mail SMTP
 */
public class EmailSender {
    private static final String TAG = "EmailSender";
    
    private Context context;
    private EmailSettingsManager settingsManager;
    
    public EmailSender(Context context) {
        this.context = context.getApplicationContext();
        this.settingsManager = new EmailSettingsManager(context);
    }
    
    /**
     * Interface for email sending callbacks
     */
    public interface EmailSendCallback {
        void onSuccess();
        void onFailure(String error);
    }
    
    /**
     * Send verification code email asynchronously
     */
    public void sendVerificationCodeEmail(String verificationCode, String smsContent, String sender, EmailSendCallback callback) {
        try {
            EmailConfig config = settingsManager.loadEmailConfig();

            if (!config.isValid() || !config.isEnabled()) {
                String error = "Email configuration is invalid or disabled";
                Log.w(TAG, error + " - Config: " + config.toString());
                if (callback != null) {
                    callback.onFailure(error);
                }
                return;
            }

            Log.d(TAG, "Sending verification code email - Code: " + verificationCode + ", Sender: " + sender);
            new SendEmailTask(config, verificationCode, smsContent, sender, callback).execute();
        } catch (Exception e) {
            String error = "Failed to initiate email sending: " + e.getMessage();
            Log.e(TAG, error, e);
            if (callback != null) {
                callback.onFailure(error);
            }
        }
    }
    
    /**
     * Test email connection and send a test email
     */
    public void sendTestEmail(EmailConfig config, EmailSendCallback callback) {
        if (!config.isValid()) {
            Log.e(TAG, "Email configuration is invalid for testing");
            if (callback != null) {
                callback.onFailure("Email configuration is invalid");
            }
            return;
        }
        
        new SendTestEmailTask(config, callback).execute();
    }
    
    /**
     * AsyncTask for sending verification code emails
     */
    private static class SendEmailTask extends AsyncTask<Void, Void, String> {
        private EmailConfig config;
        private String verificationCode;
        private String smsContent;
        private String sender;
        private EmailSendCallback callback;
        
        public SendEmailTask(EmailConfig config, String verificationCode, String smsContent, String sender, EmailSendCallback callback) {
            this.config = config;
            this.verificationCode = verificationCode;
            this.smsContent = smsContent;
            this.sender = sender;
            this.callback = callback;
        }
        
        @Override
        protected String doInBackground(Void... voids) {
            return sendVerificationEmailWithRetry();
        }

        /**
         * Send verification email with retry mechanism
         */
        private String sendVerificationEmailWithRetry() {
            String lastError = null;

            // Try TLS first (port 587)
            lastError = attemptVerificationEmailSend(false);
            if (lastError == null) {
                return null; // Success
            }

            Log.w(TAG, "TLS attempt failed, trying SSL: " + lastError);

            // If TLS fails, try SSL (port 465)
            lastError = attemptVerificationEmailSend(true);
            if (lastError == null) {
                return null; // Success
            }

            Log.e(TAG, "Both TLS and SSL attempts failed");
            return formatErrorMessage(lastError);
        }

        /**
         * Attempt to send verification email with specific configuration
         */
        private String attemptVerificationEmailSend(boolean useSSL) {
            try {
                Log.d(TAG, "Attempting verification email send with " + (useSSL ? "SSL" : "TLS"));

                // Create email session
                Session session = createEmailSession(config, useSSL);
                Log.d(TAG, "Email session created successfully");

                // Create message
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(config.getSenderEmail()));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.getRecipientEmail()));

                // Set subject
                String subject = "SMS Verification Code: " + verificationCode;
                message.setSubject(subject);

                // Create email body
                String emailBody = createVerificationEmailBody(verificationCode, smsContent, sender);
                message.setText(emailBody);

                Log.d(TAG, "Email message prepared - From: " + config.getSenderEmail() + ", To: " + config.getRecipientEmail());

                // Send email
                Transport.send(message);

                Log.d(TAG, "Verification code email sent successfully using " + (useSSL ? "SSL" : "TLS"));
                return null; // Success

            } catch (Exception e) {
                String error = "Failed to send verification email using " + (useSSL ? "SSL" : "TLS") + ": " + e.getMessage();
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
     * AsyncTask for sending test emails
     */
    private static class SendTestEmailTask extends AsyncTask<Void, Void, String> {
        private EmailConfig config;
        private EmailSendCallback callback;
        
        public SendTestEmailTask(EmailConfig config, EmailSendCallback callback) {
            this.config = config;
            this.callback = callback;
        }
        
        @Override
        protected String doInBackground(Void... voids) {
            return sendTestEmailWithRetry();
        }

        /**
         * Send test email with retry mechanism
         */
        private String sendTestEmailWithRetry() {
            String lastError = null;

            // Try TLS first (port 587)
            lastError = attemptTestEmailSend(false);
            if (lastError == null) {
                return null; // Success
            }

            Log.w(TAG, "TLS attempt failed, trying SSL: " + lastError);

            // If TLS fails, try SSL (port 465)
            lastError = attemptTestEmailSend(true);
            if (lastError == null) {
                return null; // Success
            }

            Log.e(TAG, "Both TLS and SSL attempts failed");
            return formatErrorMessage(lastError);
        }

        /**
         * Attempt to send test email with specific configuration
         */
        private String attemptTestEmailSend(boolean useSSL) {
            try {
                Log.d(TAG, "Attempting test email send with " + (useSSL ? "SSL" : "TLS"));

                // Create email session
                Session session = createEmailSession(config, useSSL);
                Log.d(TAG, "Email session created successfully");

                // Create test message
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(config.getSenderEmail()));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.getRecipientEmail()));
                message.setSubject("SMS Forward Test Email");

                String testBody = "This is a test email from SMS Forward app.\n\n" +
                        "If you receive this email, your email configuration is working correctly.\n\n" +
                        "Connection method: " + (useSSL ? "SSL (port 465)" : "TLS (port 587)") + "\n" +
                        "Sent at: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                message.setText(testBody);

                Log.d(TAG, "Test email message prepared - From: " + config.getSenderEmail() + ", To: " + config.getRecipientEmail());

                // Send email
                Transport.send(message);

                Log.d(TAG, "Test email sent successfully using " + (useSSL ? "SSL" : "TLS"));
                return null; // Success

            } catch (Exception e) {
                String error = "Failed to send test email using " + (useSSL ? "SSL" : "TLS") + ": " + e.getMessage();
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
     * Format error message with helpful suggestions
     */
    private static String formatErrorMessage(String originalError) {
        if (originalError.contains("timeout") || originalError.contains("ETIMEDOUT")) {
            return "网络连接超时。请检查：\n" +
                   "1. 网络连接是否正常\n" +
                   "2. 是否连接了VPN或代理\n" +
                   "3. 防火墙是否阻止了邮件端口\n" +
                   "原始错误：" + originalError;
        } else if (originalError.contains("authentication") || originalError.contains("535")) {
            return "邮箱认证失败。请检查：\n" +
                   "1. QQ邮箱地址是否正确\n" +
                   "2. 授权码是否正确（不是QQ密码）\n" +
                   "3. 是否已在QQ邮箱中启用SMTP服务\n" +
                   "原始错误：" + originalError;
        } else if (originalError.contains("connect")) {
            return "无法连接到邮件服务器。请检查：\n" +
                   "1. 网络连接状态\n" +
                   "2. 是否使用了网络代理\n" +
                   "3. 运营商是否限制了邮件端口\n" +
                   "原始错误：" + originalError;
        } else {
            return "邮件发送失败：" + originalError;
        }
    }

    /**
     * Create email session with QQ Mail SMTP configuration and enhanced error handling
     */
    private static Session createEmailSession(EmailConfig config) {
        return createEmailSession(config, false);
    }

    /**
     * Create email session with option to use SSL instead of TLS
     */
    private static Session createEmailSession(EmailConfig config, boolean useSSL) {
        Properties props = new Properties();

        if (useSSL) {
            // SSL configuration (port 465)
            props.put("mail.smtp.host", EmailConfig.QQ_SMTP_HOST);
            props.put("mail.smtp.port", String.valueOf(EmailConfig.QQ_SMTP_SSL_PORT));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.required", "true");
        } else {
            // TLS configuration (port 587)
            props.put("mail.smtp.host", EmailConfig.QQ_SMTP_HOST);
            props.put("mail.smtp.port", String.valueOf(EmailConfig.QQ_SMTP_PORT));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        // Enhanced timeout and connection settings
        props.put("mail.smtp.connectiontimeout", "10000"); // 10 seconds connection timeout
        props.put("mail.smtp.timeout", "10000"); // 10 seconds read timeout
        props.put("mail.smtp.writetimeout", "10000"); // 10 seconds write timeout

        // Additional reliability settings
        props.put("mail.smtp.ssl.trust", EmailConfig.QQ_SMTP_HOST);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Log.d(TAG, "Creating email session with " + (useSSL ? "SSL" : "TLS") + " configuration");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getSenderEmail(), config.getSenderPassword());
            }
        });
    }
    
    /**
     * Create formatted email body for verification code
     */
    private static String createVerificationEmailBody(String verificationCode, String smsContent, String sender) {
        StringBuilder body = new StringBuilder();
        body.append("SMS Verification Code Received\n");
        body.append("================================\n\n");
        body.append("Verification Code: ").append(verificationCode).append("\n\n");
        body.append("SMS Details:\n");
        body.append("- Sender: ").append(sender != null ? sender : "Unknown").append("\n");
        body.append("- Received: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
        body.append("- Full Message: ").append(smsContent).append("\n\n");
        body.append("This email was automatically sent by SMS Forward app.");
        
        return body.toString();
    }
}
