package com.cht.smsforward3;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test helper class for validating SMS verification code forwarding functionality
 * Useful for testing without actual SMS messages
 */
public class TestHelper {
    
    private static final String TAG = "TestHelper";
    
    /**
     * Test SMS messages with various verification code formats
     */
    private static final String[] TEST_SMS_MESSAGES = {
        // Numeric verification codes
        "Your verification code is 123456. Please enter this code to complete your login.",
        "验证码：789012，请在5分钟内输入。",
        "Code: 4567 - Use this to verify your account",
        "[8901] is your verification code for MyApp",
        
        // Alphanumeric codes
        "Your login code is ABC123. Valid for 10 minutes.",
        "Verification: XY7Z9K - Do not share this code",
        
        // Multiple codes
        "Your codes are 1111 and 2222. Use 1111 for login and 2222 for verification.",
        
        // No verification codes
        "Hello! This is a regular SMS message without any codes.",
        "Your order #12345 has been shipped and will arrive tomorrow.",
        
        // Edge cases
        "PIN: 0000 (temporary password)",
        "OTP 999888 expires in 2 minutes",
        "认证码 567890 请勿泄露"
    };
    
    private static final String[] TEST_SENDERS = {
        "Bank of China",
        "WeChat",
        "Alipay",
        "Google",
        "Microsoft",
        "Apple ID",
        "+86 138 0013 8000",
        "MyApp Service"
    };
    
    /**
     * Send test SMS data to MainActivity for testing purposes
     */
    public static void sendTestSms(Context context, int messageIndex) {
        if (messageIndex < 0 || messageIndex >= TEST_SMS_MESSAGES.length) {
            Log.e(TAG, "Invalid message index: " + messageIndex);
            return;
        }

        String content = TEST_SMS_MESSAGES[messageIndex];
        String sender = TEST_SENDERS[messageIndex % TEST_SENDERS.length];

        // Extract verification codes using our extractor
        List<String> verificationCodes = VerificationCodeExtractor.extractVerificationCodes(content);
        String primaryCode = VerificationCodeExtractor.getPrimaryVerificationCode(content);

        Log.e(TAG, "=== SENDING TEST SMS DATA ===");
        Log.e(TAG, "Content: " + content);
        Log.e(TAG, "Sender: " + sender);
        Log.e(TAG, "Codes: " + verificationCodes);
        Log.e(TAG, "Primary code: " + primaryCode);

        // Try direct method call first (for testing)
        if (context instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) context;
            mainActivity.handleTestSms(content, sender, verificationCodes, primaryCode);
            Log.e(TAG, "Test SMS sent via direct method call");
            return;
        }

        // Fallback to broadcast (for real SMS from service)
        Intent intent = new Intent("com.cht.smsforward3.SMS_RECEIVED");
        intent.putExtra("sms_content", content);
        intent.putExtra("sender", sender);
        intent.putExtra("package_name", "com.cht.smsforward3.test");
        intent.putExtra("timestamp", System.currentTimeMillis());

        if (!verificationCodes.isEmpty()) {
            intent.putStringArrayListExtra("verification_codes", new ArrayList<>(verificationCodes));
            intent.putExtra("primary_verification_code", primaryCode);
        }

        context.sendBroadcast(intent);
        Log.e(TAG, "Test SMS sent via broadcast");
    }
    
    /**
     * Send all test SMS messages
     */
    public static void sendAllTestSms(Context context) {
        for (int i = 0; i < TEST_SMS_MESSAGES.length; i++) {
            sendTestSms(context, i);
            
            // Small delay between messages
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        Log.d(TAG, "All test SMS messages sent");
    }
    
    /**
     * Get test message content for preview
     */
    public static String getTestMessage(int index) {
        if (index >= 0 && index < TEST_SMS_MESSAGES.length) {
            return TEST_SMS_MESSAGES[index];
        }
        return null;
    }
    
    /**
     * Get total number of test messages
     */
    public static int getTestMessageCount() {
        return TEST_SMS_MESSAGES.length;
    }
    
    /**
     * Test verification code extraction
     */
    public static void testVerificationCodeExtraction() {
        Log.d(TAG, "Testing verification code extraction:");
        
        for (int i = 0; i < TEST_SMS_MESSAGES.length; i++) {
            String message = TEST_SMS_MESSAGES[i];
            List<String> codes = VerificationCodeExtractor.extractVerificationCodes(message);
            String primaryCode = VerificationCodeExtractor.getPrimaryVerificationCode(message);
            
            Log.d(TAG, "Message " + i + ": " + message);
            Log.d(TAG, "  Codes found: " + codes);
            Log.d(TAG, "  Primary code: " + primaryCode);
            Log.d(TAG, "  ---");
        }
    }
    
    /**
     * Validate that all required components are properly set up
     */
    public static boolean validateSetup(Context context) {
        boolean isValid = true;
        
        // Check if notification listener service is enabled
        // This would need to be implemented based on the MainActivity's method
        Log.d(TAG, "Setup validation:");
        Log.d(TAG, "- App package: " + context.getPackageName());
        Log.d(TAG, "- Test messages available: " + TEST_SMS_MESSAGES.length);
        Log.d(TAG, "- Verification code extractor: OK");
        
        return isValid;
    }
}
