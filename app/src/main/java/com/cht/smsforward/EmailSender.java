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
            try {
                Log.d(TAG, "Starting email send task - Verification code: " + verificationCode);

                // Create email session
                Session session = createEmailSession(config);
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

                Log.d(TAG, "Verification code email sent successfully");
                return null; // Success
            } catch (MessagingException e) {
                String error = "Email messaging error: " + e.getMessage();
                Log.e(TAG, error, e);
                return error;
            } catch (Exception e) {
                String error = "Unexpected error sending email: " + e.getMessage();
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
            try {
                // Create email session
                Session session = createEmailSession(config);
                
                // Create test message
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(config.getSenderEmail()));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.getRecipientEmail()));
                message.setSubject("SMS Forward Test Email");
                
                String testBody = "This is a test email from SMS Forward app.\n\n" +
                        "If you receive this email, your email configuration is working correctly.\n\n" +
                        "Sent at: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                message.setText(testBody);
                
                // Send email
                Transport.send(message);
                
                Log.d(TAG, "Test email sent successfully");
                return null; // Success
            } catch (Exception e) {
                Log.e(TAG, "Failed to send test email", e);
                return e.getMessage();
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
     * Create email session with QQ Mail SMTP configuration
     */
    private static Session createEmailSession(EmailConfig config) {
        Properties props = new Properties();
        props.put("mail.smtp.host", EmailConfig.QQ_SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(EmailConfig.QQ_SMTP_PORT));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        
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
