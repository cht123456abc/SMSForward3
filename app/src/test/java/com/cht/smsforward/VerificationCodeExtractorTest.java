package com.cht.smsforward;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Test class for VerificationCodeExtractor to validate the optimization fixes
 */
public class VerificationCodeExtractorTest {

    @Test
    public void testProblem1_4DigitCodeWithChineseKeyword() {
        // Problem 1: 4位数验证码识别失败
        String smsContent = "【云测】您的验证码为2354，请于5分钟内正确输入，如非本人操作，请忽略此短信。";
        
        List<String> codes = VerificationCodeExtractor.extractVerificationCodes(smsContent);
        String primaryCode = VerificationCodeExtractor.getPrimaryVerificationCode(smsContent);
        
        // Should find the verification code "2354"
        assertFalse("Should find verification codes", codes.isEmpty());
        assertTrue("Should contain code 2354", codes.contains("2354"));
        assertEquals("Primary code should be 2354", "2354", primaryCode);
        
        System.out.println("Test 1 - Found codes: " + codes);
        System.out.println("Test 1 - Primary code: " + primaryCode);
    }

    @Test
    public void testProblem2_NonVerificationSmsWithUrl() {
        // Problem 2: 非验证码短信被误判
        String smsContent = "【中国电信】亲，又到月末啦，每月2日为账单日，请关注您的账户余额，避免余额不足影响您的正常使用。回复数字：102 查询余额 103 查询账单 充值享折扣，请点击： https://m.sc.189.cn/su/0/QuQ1nd 登录中国电信APP进行充值。满意服务，10分信赖！";
        
        List<String> codes = VerificationCodeExtractor.extractVerificationCodes(smsContent);
        String primaryCode = VerificationCodeExtractor.getPrimaryVerificationCode(smsContent);
        
        // Should NOT find any verification codes (especially not "QuQ1nd")
        assertTrue("Should not find any verification codes", codes.isEmpty());
        assertNull("Primary code should be null", primaryCode);
        assertFalse("Should not contain QuQ1nd", codes.contains("QuQ1nd"));
        
        System.out.println("Test 2 - Found codes: " + codes);
        System.out.println("Test 2 - Primary code: " + primaryCode);
    }

    @Test
    public void testValidVerificationCodes() {
        // Test various valid verification code formats
        String[] testCases = {
            "您的验证码为1234，请及时输入",
            "Your verification code is 5678",
            "验证码：9876，有效期5分钟",
            "Code: ABC123",
            "PIN: 4567"
        };
        
        String[] expectedCodes = {"1234", "5678", "9876", "ABC123", "4567"};
        
        for (int i = 0; i < testCases.length; i++) {
            List<String> codes = VerificationCodeExtractor.extractVerificationCodes(testCases[i]);
            assertFalse("Should find codes for: " + testCases[i], codes.isEmpty());
            assertTrue("Should contain expected code: " + expectedCodes[i], 
                      codes.contains(expectedCodes[i]));
            System.out.println("Valid test " + (i+1) + " - SMS: " + testCases[i]);
            System.out.println("Valid test " + (i+1) + " - Found codes: " + codes);
        }
    }

    @Test
    public void testNonVerificationSms() {
        // Test various non-verification SMS that should not trigger false positives
        String[] testCases = {
            "【银行】您的账户余额为1234.56元，请及时充值",
            "【购物】订单号：ABC123，商品已发货",
            "【广告】点击链接 http://example.com/promo123 获取优惠",
            "【通知】会议室预订成功，房间号：A101"
        };
        
        for (String testCase : testCases) {
            List<String> codes = VerificationCodeExtractor.extractVerificationCodes(testCase);
            System.out.println("Non-verification test - SMS: " + testCase);
            System.out.println("Non-verification test - Found codes: " + codes);
            // These might find some codes but should be minimal and not include obvious non-codes
        }
    }

    @Test
    public void testEdgeCases() {
        // Test edge cases
        String[] testCases = {
            "", // Empty string
            "短信内容没有任何数字", // No numbers
            "只有三位数123", // Too short
            "这是一个很长的数字123456789", // Too long
            "验证码为：", // Keyword but no code
        };
        
        for (String testCase : testCases) {
            List<String> codes = VerificationCodeExtractor.extractVerificationCodes(testCase);
            System.out.println("Edge case test - SMS: '" + testCase + "'");
            System.out.println("Edge case test - Found codes: " + codes);
        }
    }
}
