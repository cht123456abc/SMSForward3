# SMS Verification Code Forwarder - Testing Guide

## Testing on Meizu 20 (Android 14)

### Prerequisites
1. **Device**: Meizu 20 running Android 14
2. **Development**: Android Studio with USB debugging enabled
3. **Test SMS**: Access to send SMS messages to your device

### Step-by-Step Testing Process

#### Phase 1: Installation and Basic Setup
1. **Install the app** on your Meizu 20 device
2. **Launch the app** - you should see the main interface
3. **Check initial status** - app should show "Notification access required"

#### Phase 2: Permission Setup
1. **Tap "Enable Notification Access"** button
2. **Verify Settings redirect** - should open Android notification access settings
3. **Enable notification access** for "SMSForward" app
4. **Return to app** - status should change to "✓ Notification access enabled"

#### Phase 3: Basic Functionality Test
1. **Tap "Send Test SMS"** button
2. **Verify test message appears** in the list with:
   - Sender information
   - Timestamp
   - Message content
   - Highlighted verification code (123456)
   - Copy button for verification code

#### Phase 4: Real SMS Testing
1. **Send yourself an SMS** with a verification code from another device or service
2. **Common test formats**:
   - "Your verification code is 123456"
   - "验证码：789012"
   - "Code: 4567"
   - "[8901] verification code"

#### Phase 5: Meizu SMS App Compatibility
1. **Use Meizu's default SMS app** to receive messages
2. **Test with different SMS sources**:
   - Banking apps
   - Social media platforms
   - E-commerce services
   - Government services

### Expected Behavior

#### ✅ Successful Operation
- App shows "Notification access enabled" status
- Test SMS appears immediately in the list
- Real SMS messages appear within 1-2 seconds
- Verification codes are highlighted in yellow
- Copy button works for verification codes
- Toast notifications appear for new codes

#### ❌ Common Issues and Solutions

**Issue**: App shows "Notification access required" even after enabling
- **Solution**: Restart the app or check if permission was actually granted
- **Check**: Go to Settings > Apps > Special access > Notification access

**Issue**: Test SMS works but real SMS doesn't appear
- **Solution**: Check if Meizu SMS app is supported
- **Debug**: Look for package name in logs (should be com.meizu.flyme.mms)

**Issue**: SMS appears but verification codes not highlighted
- **Solution**: Check if SMS contains recognizable verification code patterns
- **Test**: Use standard formats like "Code: 1234" or "验证码：5678"

**Issue**: App crashes when receiving SMS
- **Solution**: Check Android Studio logcat for error messages
- **Debug**: Look for null pointer exceptions or broadcast receiver issues

### Debugging Tools

#### LogCat Monitoring
Monitor these log tags in Android Studio:
- `MainActivity`: UI updates and permission status
- `SmsNotificationListener`: SMS notification interception
- `VerificationCodeExtractor`: Code extraction process
- `TestHelper`: Test message functionality

#### Key Log Messages to Look For
```
✅ "SmsNotificationListener service created"
✅ "SMS notification detected from: com.meizu.flyme.mms"
✅ "SMS content extracted: [message content]"
✅ "Verification codes found: [codes]"
✅ "SMS content broadcasted"
✅ "SMS broadcast received"
✅ "SMS added to UI"
```

### Performance Validation

#### Response Time
- **Test SMS**: Should appear instantly (< 100ms)
- **Real SMS**: Should appear within 1-2 seconds
- **UI Updates**: Should be smooth without lag

#### Memory Usage
- **Normal operation**: < 50MB RAM usage
- **No memory leaks**: Check after receiving 10+ SMS messages

#### Battery Impact
- **Minimal drain**: Service should not significantly impact battery
- **Background efficiency**: App should work when in background

### Meizu-Specific Considerations

#### Meizu SMS App Packages
The app supports these Meizu SMS package names:
- `com.meizu.flyme.mms` (primary)
- `com.meizu.mms` (alternative)

#### Meizu System Optimizations
1. **Battery optimization**: Ensure app is not optimized for battery
2. **Background app limits**: Check if app can run in background
3. **Notification permissions**: Verify all notification permissions are granted

### Test Cases Checklist

#### Basic Functionality
- [ ] App installs successfully
- [ ] Permission request works
- [ ] Test SMS button works
- [ ] UI updates correctly

#### SMS Interception
- [ ] Numeric codes (4-8 digits)
- [ ] Alphanumeric codes
- [ ] Chinese verification messages
- [ ] Multiple codes in one message
- [ ] Messages without codes

#### UI Features
- [ ] Message list scrolling
- [ ] Copy to clipboard
- [ ] Toast notifications
- [ ] Empty state display
- [ ] Status indicators

#### Edge Cases
- [ ] App restart after receiving SMS
- [ ] Background operation
- [ ] Multiple rapid SMS messages
- [ ] Very long SMS content
- [ ] Special characters in SMS

### Troubleshooting Commands

#### ADB Commands for Debugging
```bash
# Check if notification listener is enabled
adb shell settings get secure enabled_notification_listeners

# Send test notification (if needed)
adb shell am broadcast -a com.cht.smsforward.SMS_RECEIVED

# Check app logs
adb logcat | grep -E "(MainActivity|SmsNotificationListener|VerificationCodeExtractor)"
```

### Success Criteria
The implementation is successful if:
1. ✅ Notification access permission works correctly
2. ✅ Test SMS appears in the app immediately
3. ✅ Real SMS from Meizu SMS app are intercepted
4. ✅ Verification codes are extracted and highlighted
5. ✅ Copy to clipboard functionality works
6. ✅ App works reliably in background
7. ✅ No crashes or memory leaks
8. ✅ Minimal battery impact

### Next Steps After Successful Testing
1. **Production use**: Start using for real verification codes
2. **Customization**: Adjust regex patterns if needed
3. **Additional features**: Consider adding SMS filtering or forwarding
4. **Backup**: Export important verification codes if needed
