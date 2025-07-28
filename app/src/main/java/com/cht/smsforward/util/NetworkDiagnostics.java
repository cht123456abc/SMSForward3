package com.cht.smsforward.util;

import com.cht.smsforward.config.EmailConfig;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 网络诊断工具类，用于检测邮件服务器连接问题
 */
public class NetworkDiagnostics {
    
    private static final String TAG = "NetworkDiagnostics";
    private static final int TIMEOUT_MS = 5000; // 5秒超时
    
    public interface DiagnosticsCallback {
        void onResult(DiagnosticsResult result);
    }
    
    public static class DiagnosticsResult {
        public boolean networkAvailable;
        public boolean tlsPortOpen;
        public boolean sslPortOpen;
        public String networkType;
        public String errorMessage;
        public String suggestions;
        
        public DiagnosticsResult() {
            this.networkAvailable = false;
            this.tlsPortOpen = false;
            this.sslPortOpen = false;
            this.networkType = "Unknown";
            this.errorMessage = "";
            this.suggestions = "";
        }
        
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("网络诊断结果：\n");
            sb.append("网络连接：").append(networkAvailable ? "✓ 正常" : "✗ 异常").append("\n");
            sb.append("网络类型：").append(networkType).append("\n");
            sb.append("TLS端口(587)：").append(tlsPortOpen ? "✓ 可达" : "✗ 不可达").append("\n");
            sb.append("SSL端口(465)：").append(sslPortOpen ? "✓ 可达" : "✗ 不可达").append("\n");
            
            if (!errorMessage.isEmpty()) {
                sb.append("\n错误信息：").append(errorMessage).append("\n");
            }
            
            if (!suggestions.isEmpty()) {
                sb.append("\n建议：").append(suggestions);
            }
            
            return sb.toString();
        }
    }
    
    /**
     * 执行网络诊断
     */
    public static void runDiagnostics(Context context, DiagnosticsCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            DiagnosticsResult result = new DiagnosticsResult();
            
            try {
                // 检查网络连接
                checkNetworkConnectivity(context, result);
                
                // 检查邮件服务器端口连接
                checkEmailServerPorts(result);
                
                // 生成建议
                generateSuggestions(result);
                
            } catch (Exception e) {
                result.errorMessage = "诊断过程中发生错误：" + e.getMessage();
                Log.e(TAG, "Diagnostics error", e);
            }
            
            // 回调结果
            if (callback != null) {
                callback.onResult(result);
            }
        });
        
        executor.shutdown();
    }
    
    /**
     * 检查网络连接状态
     */
    private static void checkNetworkConnectivity(Context context, DiagnosticsResult result) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null && activeNetwork.isConnected()) {
                    result.networkAvailable = true;
                    result.networkType = activeNetwork.getTypeName();
                    Log.d(TAG, "Network available: " + result.networkType);
                } else {
                    result.networkAvailable = false;
                    result.errorMessage = "无网络连接";
                    Log.w(TAG, "No network connection");
                }
            }
        } catch (Exception e) {
            result.errorMessage = "检查网络连接时出错：" + e.getMessage();
            Log.e(TAG, "Error checking network connectivity", e);
        }
    }
    
    /**
     * 检查邮件服务器端口连接
     */
    private static void checkEmailServerPorts(DiagnosticsResult result) {
        // 检查TLS端口 (587)
        result.tlsPortOpen = checkPortConnectivity(EmailConfig.QQ_SMTP_HOST, EmailConfig.QQ_SMTP_PORT);
        
        // 检查SSL端口 (465)
        result.sslPortOpen = checkPortConnectivity(EmailConfig.QQ_SMTP_HOST, EmailConfig.QQ_SMTP_SSL_PORT);
        
        Log.d(TAG, "Port connectivity - TLS(587): " + result.tlsPortOpen + ", SSL(465): " + result.sslPortOpen);
    }
    
    /**
     * 检查特定端口的连接性
     */
    private static boolean checkPortConnectivity(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            Log.d(TAG, "Successfully connected to " + host + ":" + port);
            return true;
        } catch (SocketTimeoutException e) {
            Log.w(TAG, "Timeout connecting to " + host + ":" + port);
            return false;
        } catch (IOException e) {
            Log.w(TAG, "Failed to connect to " + host + ":" + port + " - " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 根据诊断结果生成建议
     */
    private static void generateSuggestions(DiagnosticsResult result) {
        StringBuilder suggestions = new StringBuilder();
        
        if (!result.networkAvailable) {
            suggestions.append("• 请检查网络连接\n");
            suggestions.append("• 尝试切换WiFi或移动数据\n");
        } else if (!result.tlsPortOpen && !result.sslPortOpen) {
            suggestions.append("• 邮件服务器端口被阻止，可能原因：\n");
            suggestions.append("  - 防火墙或安全软件阻止\n");
            suggestions.append("  - 运营商限制邮件端口\n");
            suggestions.append("  - 公司或学校网络限制\n");
            suggestions.append("• 建议尝试：\n");
            suggestions.append("  - 切换到移动数据网络\n");
            suggestions.append("  - 关闭VPN或代理\n");
            suggestions.append("  - 联系网络管理员\n");
        } else if (!result.tlsPortOpen) {
            suggestions.append("• TLS端口(587)不可达，但SSL端口(465)可用\n");
            suggestions.append("• 应用会自动使用SSL连接\n");
        } else if (!result.sslPortOpen) {
            suggestions.append("• SSL端口(465)不可达，但TLS端口(587)可用\n");
            suggestions.append("• 应用会使用TLS连接\n");
        } else {
            suggestions.append("• 网络连接正常\n");
            suggestions.append("• 如果仍有问题，请检查邮箱配置\n");
        }
        
        result.suggestions = suggestions.toString();
    }
}
