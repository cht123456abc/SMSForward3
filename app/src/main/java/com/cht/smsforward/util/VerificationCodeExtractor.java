package com.cht.smsforward.util;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for extracting and highlighting verification codes from SMS content
 * Supports various verification code formats commonly used in SMS messages
 */
public class VerificationCodeExtractor {
    
    private static final String TAG = "VerificationCodeExtractor";
    
    // Regex patterns for different types of verification codes
    private static final Pattern[] VERIFICATION_PATTERNS = {
        // Codes with specific keywords (highest priority)
        Pattern.compile("(?i)(?:code|verification|verify|pin|otp)[:\\s]*([A-Za-z0-9]{4,8})"),

        // Chinese/international verification code patterns - enhanced for "为" keyword
        Pattern.compile("(?i)(?:验证码|驗證碼|認證碼|认证码)(?:为|為|是|：|:)\\s*([A-Za-z0-9]{4,8})"),

        // Additional Chinese pattern for "您的验证码为XXXX" format
        Pattern.compile("(?i)您的验证码为([A-Za-z0-9]{4,8})"),

        // Codes in parentheses or brackets
        Pattern.compile("[\\(\\[]([A-Za-z0-9]{4,8})[\\)\\]]"),

        // Codes after colon or dash
        Pattern.compile("[:：-]\\s*([A-Za-z0-9]{4,8})\\b"),

        // 4-8 digit numbers (most common, but lower priority to avoid false positives)
        Pattern.compile("\\b\\d{4,8}\\b"),

        // Alphanumeric codes (lowest priority)
        Pattern.compile("\\b[A-Za-z0-9]{4,8}\\b"),
    };
    
    // Keywords that indicate verification codes (for context filtering)
    private static final String[] VERIFICATION_KEYWORDS = {
        "verification", "verify", "code", "pin", "otp", "auth",
        "验证码", "驗證碼", "認證碼", "认证码"
    };
    
    // Colors for highlighting (will be used in UI)
    public static final int HIGHLIGHT_BACKGROUND_COLOR = 0xFFFFEB3B; // Yellow background
    public static final int HIGHLIGHT_TEXT_COLOR = 0xFF000000;       // Black text
    
    /**
     * Extract all potential verification codes from SMS content (优化版本)
     */
    public static List<String> extractVerificationCodes(String smsContent) {
        List<String> codes = new ArrayList<>();

        if (smsContent == null || smsContent.trim().isEmpty()) {
            return codes;
        }

        // 性能优化：预先检查长度，避免处理过长的消息
        if (smsContent.length() > 500) {
            smsContent = smsContent.substring(0, 500); // 截取前500字符
        }

        // Step 1: 快速关键词检查
        boolean hasVerificationContext = hasVerificationKeywords(smsContent);
        if (!hasVerificationContext) {
            return codes; // 早期返回，避免不必要的正则处理
        }

        // Step 2: 优化的模式匹配 - 按优先级顺序，找到第一个匹配就停止某些模式
        boolean foundHighPriorityCode = false;

        for (int i = 0; i < VERIFICATION_PATTERNS.length; i++) {
            Pattern pattern = VERIFICATION_PATTERNS[i];

            // 对于高优先级模式（前3个），如果已经找到代码就跳过低优先级模式
            if (foundHighPriorityCode && i >= 3) {
                break;
            }

            Matcher matcher = pattern.matcher(smsContent);

            while (matcher.find()) {
                String code;

                // Some patterns have groups, others match the entire pattern
                if (matcher.groupCount() > 0) {
                    code = matcher.group(1); // Get the captured group
                } else {
                    code = matcher.group(0); // Get the entire match
                }

                if (code != null && isValidVerificationCode(code, hasVerificationContext)) {
                    if (!codes.contains(code)) {
                        codes.add(code);

                        // 如果是高优先级模式（前3个）找到了代码，标记为已找到
                        if (i < 3) {
                            foundHighPriorityCode = true;
                        }

                        // 性能优化：如果找到了明确的验证码，不需要继续搜索
                        if (i < 2 && code.matches("\\d{4,8}")) {
                            return codes; // 早期返回
                        }
                    }
                }
            }
        }

        return codes;
    }
    
    /**
     * Create highlighted text with verification codes marked (优化版本)
     */
    public static SpannableString createHighlightedText(String smsContent) {
        SpannableString spannableString = new SpannableString(smsContent);

        if (smsContent == null || smsContent.trim().isEmpty()) {
            return spannableString;
        }

        // 性能优化：只在UI需要时才进行高亮处理
        // 如果没有验证码关键词，直接返回原始文本
        if (!hasVerificationKeywords(smsContent)) {
            return spannableString;
        }

        List<String> codes = extractVerificationCodes(smsContent);

        for (String code : codes) {
            highlightCodeInText(spannableString, code);
        }

        return spannableString;
    }
    
    /**
     * Highlight a specific verification code in the text
     */
    private static void highlightCodeInText(SpannableString spannableString, String code) {
        String text = spannableString.toString();
        int startIndex = 0;
        
        while (startIndex < text.length()) {
            int index = text.indexOf(code, startIndex);
            if (index == -1) {
                break;
            }
            
            // Check if this is a word boundary match (not part of a larger word)
            boolean isWordBoundary = (index == 0 || !Character.isLetterOrDigit(text.charAt(index - 1))) &&
                                   (index + code.length() == text.length() || 
                                    !Character.isLetterOrDigit(text.charAt(index + code.length())));
            
            if (isWordBoundary) {
                // Apply highlighting
                spannableString.setSpan(
                    new BackgroundColorSpan(HIGHLIGHT_BACKGROUND_COLOR),
                    index,
                    index + code.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                
                spannableString.setSpan(
                    new ForegroundColorSpan(HIGHLIGHT_TEXT_COLOR),
                    index,
                    index + code.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                
                // Log.d(TAG, "Highlighted verification code: " + code + " at position " + index);
            }
            
            startIndex = index + 1;
        }
    }
    
    /**
     * Check if SMS content contains verification-related keywords (优化版本)
     */
    private static boolean hasVerificationKeywords(String smsContent) {
        // 性能优化：避免重复的toLowerCase调用
        String lowerContent = smsContent.toLowerCase();

        // 优化：使用更高效的字符串搜索
        for (String keyword : VERIFICATION_KEYWORDS) {
            if (lowerContent.indexOf(keyword.toLowerCase()) != -1) {
                return true;
            }
        }

        return false;
    }


    
    /**
     * Validate if a potential code is actually a verification code
     */
    private static boolean isValidVerificationCode(String code, boolean hasVerificationContext) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }

        code = code.trim();

        // Length check
        if (code.length() < 4 || code.length() > 8) {
            return false;
        }

        // Filter out common English words that might match patterns
        String lowerCode = code.toLowerCase();
        if (lowerCode.equals("your") || lowerCode.equals("code") || lowerCode.equals("the") ||
            lowerCode.equals("this") || lowerCode.equals("that") || lowerCode.equals("with") ||
            lowerCode.equals("from") || lowerCode.equals("have") || lowerCode.equals("will") ||
            lowerCode.equals("been") || lowerCode.equals("they") || lowerCode.equals("were") ||
            lowerCode.equals("please") || lowerCode.equals("enter") || lowerCode.equals("complete") ||
            lowerCode.equals("login") || lowerCode.equals("verify") || lowerCode.equals("account") ||
            lowerCode.equals("phone") || lowerCode.equals("number") || lowerCode.equals("message")) {
            return false;
        }

        // Must contain at least one digit for verification codes
        if (!code.matches(".*\\d.*")) {
            return false;
        }

        // If no verification context, be more strict (this should not happen due to step 1 check)
        if (!hasVerificationContext) {
            return code.matches("\\d{4,8}");
        }

        // With verification context, allow alphanumeric codes
        return code.matches("[A-Za-z0-9]{4,8}");
    }

    
    /**
     * Get the most likely verification code from extracted codes
     */
    public static String getPrimaryVerificationCode(String smsContent) {
        List<String> codes = extractVerificationCodes(smsContent);
        
        if (codes.isEmpty()) {
            return null;
        }
        
        // Prefer numeric codes
        for (String code : codes) {
            if (code.matches("\\d{4,8}")) {
                return code;
            }
        }
        
        // Return the first alphanumeric code
        return codes.get(0);
    }
}
