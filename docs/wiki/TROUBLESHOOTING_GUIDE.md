# Personal Diary - Troubleshooting Guide

## 🔧 Common Issues and Solutions

This guide covers the most common issues users encounter with Personal Diary and how to resolve them.

## 🔐 Account and Authentication Issues

### Cannot Log In

#### **Symptom**: "Invalid email or password" error
**Solutions**:
1. **Double-check your email address** - no typos, correct domain
2. **Verify password** - check caps lock, try typing in a text editor first
3. **Try password reset** (UCE users only):
   - Click "Forgot Password" on login screen
   - Check email (including spam folder)
   - Follow reset instructions
4. **Use recovery codes** (E2E users):
   - Click "Use Recovery Code" on login screen
   - Enter one of your 10 recovery codes
   - Each code can only be used once

#### **Symptom**: "Account not found" error
**Solutions**:
1. **Verify you're using the correct email** address
2. **Check if you signed up on a different platform** (web vs mobile)
3. **Try alternative email addresses** you might have used
4. **Contact support** if you're certain the account exists

### Forgot Password (UCE Users Only)

#### **E2E Users**: Use recovery codes instead
1. **Click "Use Recovery Code"** on login screen
2. **Enter any unused recovery code**
3. **You'll be prompted to create a new password**
4. **Log in with your new password**

#### **UCE Users**: Email-based reset
1. **Click "Forgot Password"** on login screen
2. **Enter your email address**
3. **Check your email** (including spam/junk folders)
4. **Click the reset link** within 1 hour
5. **Create a new password** (minimum 12 characters)
6. **Log in with your new password**

### Recovery Codes Issues (E2E Users)

#### **Lost Recovery Codes**
- **Unfortunately, there is no way to recover an E2E account without recovery codes**
- **This is by design** for maximum security
- **You will need to create a new account**
- **Prevention**: Always keep recovery codes in multiple secure locations

#### **Recovery Code Not Working**
1. **Check the format**: Should be XXXX-XXXX-XXXX-XXXX
2. **Try typing instead of copy/paste**
3. **Ensure no extra spaces** before or after the code
4. **Check if the code was already used** (each code works only once)
5. **Try a different recovery code**

### Biometric Authentication Problems

#### **Face ID/Touch ID Not Working**
1. **Check if biometric authentication is enabled** in device settings
2. **Re-enroll your biometrics** in device settings
3. **Disable and re-enable** biometric auth in Personal Diary settings
4. **Use password fallback** if biometrics fail repeatedly
5. **Restart the app** and try again

#### **"Biometric Authentication Unavailable"**
1. **Check device compatibility** (iPhone 5S+, Android with fingerprint sensor)
2. **Enable biometric authentication** in device settings
3. **Enroll at least one fingerprint/face** in device settings
4. **Grant biometric permissions** to Personal Diary
5. **Update your device** to the latest OS version

## 📱 App Performance Issues

### App Crashes or Freezes

#### **General Solutions**:
1. **Force close and restart** the app
2. **Restart your device**
3. **Check available storage** (need at least 500MB free)
4. **Update the app** to the latest version
5. **Clear app cache** (Android: Settings → Apps → Personal Diary → Storage → Clear Cache)

#### **iOS Specific**:
1. **Double-tap home button** and swipe up on Personal Diary to close
2. **Go to Settings → General → iPhone Storage** and check space
3. **Restart iPhone** by holding power + volume buttons

#### **Android Specific**:
1. **Use recent apps button** and swipe away Personal Diary
2. **Go to Settings → Apps → Personal Diary → Force Stop**
3. **Clear cache** (not data unless instructed by support)

### Slow Performance

#### **Symptoms**: App is slow to load, scroll, or respond
**Solutions**:
1. **Check internet connection** (try loading a website)
2. **Close other apps** running in background
3. **Restart the app**
4. **Check device storage** (need 500MB+ free)
5. **Reduce number of entries displayed** in timeline settings
6. **Optimize media files** (compress photos in app settings)

#### **Large Database Issues**:
If you have 1000+ entries:
1. **Enable "Lazy loading"** in settings (loads entries as needed)
2. **Archive old entries** (export and delete locally)
3. **Reduce image quality** for future uploads
4. **Consider upgrading device** if very old hardware

## 🔄 Sync and Data Issues

### Entries Not Syncing

#### **Check Sync Status**:
1. **Look for sync indicator** in top bar (spinning icon or "syncing")
2. **Go to Settings → Sync** to see last sync time
3. **Try manual sync** by pulling down on timeline or tapping sync button

#### **Common Causes and Solutions**:
1. **No internet connection**:
   - Check WiFi/cellular data
   - Try loading a website
   - Move to area with better signal

2. **Background sync disabled**:
   - iOS: Settings → Personal Diary → Background App Refresh (enable)
   - Android: Settings → Apps → Personal Diary → Battery → Allow background activity

3. **Account authentication expired**:
   - Log out and log back in
   - Check if password changed recently
   - Re-enter credentials if prompted

4. **Server issues**:
   - Check status page (if available)
   - Try again in a few minutes
   - Contact support if widespread

### Duplicate Entries Appearing

#### **Causes and Solutions**:
1. **Sync conflict** from editing on multiple devices:
   - Choose which version to keep in conflict resolution dialog
   - Enable "Last modified wins" in sync settings

2. **Import duplicates** from social media:
   - Check import settings
   - Run deduplication in Settings → Social Media
   - Manually delete duplicates

3. **App crash during sync**:
   - Restart app and try manual sync
   - Duplicates usually resolve automatically

### Missing Entries

#### **Troubleshooting Steps**:
1. **Check if entries are filtered**:
   - Clear search filters in timeline
   - Check date range filters
   - Look in "All Entries" view

2. **Check sync status**:
   - Try manual sync
   - Look at last sync time in settings
   - Check internet connection

3. **Check entry source**:
   - Look in different sections (diary, imported, etc.)
   - Check if entries were accidentally deleted

4. **Account switching**:
   - Verify you're logged into the correct account
   - Check if you have multiple accounts

## 📸 Media and File Issues

### Photos/Videos Not Loading

#### **Common Solutions**:
1. **Check internet connection** (media may not be cached locally)
2. **Try manual sync** to download missing media
3. **Check available storage** for media cache
4. **Clear media cache** in settings and re-download

#### **Permission Issues**:
1. **Grant camera permissions**:
   - iOS: Settings → Privacy → Camera → Personal Diary (enable)
   - Android: Settings → Apps → Personal Diary → Permissions → Camera (allow)

2. **Grant photo library permissions**:
   - iOS: Settings → Privacy → Photos → Personal Diary (full access)
   - Android: Settings → Apps → Personal Diary → Permissions → Storage (allow)

### Cannot Take Photos/Videos

#### **Camera Issues**:
1. **Check camera permissions** (see above)
2. **Close other apps** that might be using camera
3. **Restart the app**
4. **Check if camera hardware is working** (try built-in camera app)
5. **Update the app** to latest version

#### **Storage Issues**:
1. **Check available storage** (photos/videos need space to save)
2. **Clear up device storage** by deleting unnecessary files
3. **Change photo quality** in app settings to save space

### Media Upload Failures

#### **Troubleshooting**:
1. **Check internet connection** (stable WiFi recommended for large files)
2. **Try smaller files first** (reduce video length or photo quality)
3. **Check upload queue** in settings - may be processing in background
4. **Restart upload** if stuck
5. **Try uploading one file at a time**

## 🔍 Search Issues

### Search Not Finding Entries

#### **E2E Users** (Client-side search only):
1. **Entries must be downloaded** to device to be searchable
2. **Try manual sync** to ensure all entries are local
3. **Search is exact match** - try variations of keywords
4. **Check spelling** of search terms
5. **Use tags** for more reliable finding

#### **UCE Users** (Server-side search):
1. **Check internet connection** for server search
2. **Try manual sync** to update search index
3. **Wait a few minutes** after creating entry (indexing delay)
4. **Use quotation marks** for exact phrase search
5. **Try different keywords** or partial words

### Search Results Incomplete

#### **Common Causes**:
1. **Search index needs updating**:
   - Go to Settings → Search → Rebuild Index (UCE users)
   - Try manual sync

2. **Entries not fully synced**:
   - Check sync status
   - Try manual sync
   - Verify entries exist in timeline

3. **Content not indexed**:
   - Some content types may not be searchable
   - Try searching by tags instead of content

## 🌐 Social Media Integration Issues

### Facebook Connection Problems

#### **Cannot Connect to Facebook**:
1. **Check internet connection**
2. **Try logging into Facebook** in your web browser first
3. **Clear browser cache** if using web version
4. **Update the app** to latest version
5. **Check Facebook isn't down** (facebook.com/help)

#### **Connection Expired**:
1. **Go to Settings → Social Media → Facebook**
2. **Tap "Reconnect"**
3. **Complete authorization process** again
4. **Previous imports will remain** unchanged

### Import Problems

#### **Import Stuck or Slow**:
1. **Check internet connection** (stable WiFi recommended)
2. **Try smaller date ranges** for import
3. **Close other apps** to free up memory
4. **Restart the app** and try again
5. **Check Facebook privacy settings** (posts must be accessible)

#### **Some Posts Not Imported**:
1. **Check Facebook privacy settings** for missing posts
2. **Facebook API limits** may exclude very old posts
3. **Try importing different date ranges**
4. **Some post types** (like live videos) may not be supported

## 📞 Getting Additional Help

### Before Contacting Support

1. **Try restarting the app**
2. **Check for app updates**
3. **Review this troubleshooting guide**
4. **Check internet connection**
5. **Note error messages** exactly as they appear

### Contacting Support

#### **Email Support**: support@diary.xmojo.net

**Include in your message**:
- Device type and OS version (e.g., "iPhone 13, iOS 16.2")
- App version (found in Settings → About)
- Detailed description of the problem
- Steps you've already tried
- Screenshots of error messages (if any)
- Encryption tier (E2E or UCE)

#### **Community Support**:
- **Discord server**: Real-time help from community
- **Reddit**: r/PersonalDiary for discussions
- **GitHub**: Bug reports and feature requests

### Emergency Data Recovery

#### **For E2E Users**:
- **If you lose access completely**, there is no server-side recovery
- **This is by design** for maximum privacy
- **Always keep recovery codes** in multiple secure locations

#### **For UCE Users**:
- **Data export available** in Settings → Account → Export Data
- **Account recovery possible** via email
- **Contact support** for assistance with data recovery

---

## 🛡️ Prevention Tips

### Security Best Practices:
1. **E2E users**: Keep recovery codes in multiple secure locations
2. **UCE users**: Use a strong, unique password
3. **All users**: Enable biometric authentication
4. **Regular backups**: Export your data periodically

### Performance Best Practices:
1. **Keep app updated** to latest version
2. **Maintain device storage** (500MB+ free)
3. **Regular device restarts**
4. **Close unused apps** regularly

### Data Best Practices:
1. **Sync regularly** when you have good internet
2. **Export data periodically** as backup
3. **Tag entries consistently** for better organization
4. **Review privacy settings** before social media sharing

---

**If you cannot find a solution here, don't hesitate to contact our support team. We're here to help!**