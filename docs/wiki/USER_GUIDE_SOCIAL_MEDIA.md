# Social Media Integration Guide

## 🌐 Overview

Personal Diary allows you to seamlessly integrate your digital journal with your social media presence. The core philosophy is that **your diary is the source of truth** - you create content in your private diary first, then choose what to share publicly.

## 📱 Supported Platforms

### ✅ **Facebook** (Available Now)
- Import existing posts as diary entries
- Share diary entries to Facebook
- Automatic deduplication
- Privacy controls

### 🚧 **Instagram** (Coming Soon)
- Import stories and posts
- Share photos with captions
- Story highlights integration

### 🚧 **Twitter/X** (Coming Soon)
- Tweet diary entries
- Thread creation from long entries
- Tweet import and archival

## 🔗 Facebook Integration

### Setting Up Facebook Connection

#### Web Application:
1. **Go to Settings → Social Media**
2. **Click "Connect Facebook"**
3. **A popup window will open** for Facebook authorization
4. **Log in to Facebook** if not already logged in
5. **Review permissions** requested by Personal Diary:
   - Read your posts and profile information
   - Publish posts on your behalf (only when you choose)
6. **Click "Continue"** to authorize
7. **The popup will close** and you'll see "Facebook Connected" in settings

#### Mobile Apps:
1. **Go to Settings → Social Media**
2. **Tap "Connect Facebook"**
3. **Your device's browser will open** for Facebook authorization
4. **Complete the authorization process**
5. **Return to the app** - connection will be confirmed

### Importing Facebook Posts

Once connected, you can import your existing Facebook posts as diary entries:

#### Initial Import:
1. **Go to Settings → Social Media → Facebook**
2. **Tap "Import Posts"**
3. **Choose date range** (optional):
   - All time (default)
   - Last year
   - Custom date range
4. **Select post types** to import:
   - Text posts
   - Photos with captions
   - Check-ins with notes
   - Life events
5. **Tap "Start Import"**
6. **Progress will be shown** as posts are imported
7. **Review imported entries** in your timeline

#### What Gets Imported:
- **Post text** as diary entry content
- **Photos and videos** as encrypted media attachments
- **Location data** if available
- **Post date/time** as entry timestamp
- **Automatic tag**: "facebook-import"

#### Privacy Note:
- **All imported content** is private by default
- **You control** what gets imported
- **Original Facebook posts** are unchanged

### Sharing Diary Entries to Facebook

#### From Entry Detail View:
1. **Open any diary entry**
2. **Tap the share button** (usually in the top bar)
3. **Select "Share to Facebook"**
4. **Review the content** to be shared:
   - Entry text (you can edit before sharing)
   - Photos/videos (select which ones to include)
5. **Choose privacy level**:
   - Public
   - Friends
   - Only Me
   - Custom (specific friends or groups)
6. **Add additional text** if desired
7. **Tap "Post to Facebook"**

#### From Timeline View:
1. **Long press on any entry** (mobile) or right-click (web)
2. **Select "Share to Facebook"** from the menu
3. **Follow the same process** as above

#### Sharing Options:
- **Edit before sharing**: Modify text, remove sensitive parts
- **Select media**: Choose which photos/videos to include
- **Add context**: Add additional text for Facebook audience
- **Privacy control**: Set who can see the shared post

### Advanced Facebook Features

#### Automatic Sharing (UCE Tier Only):
1. **Go to Settings → Social Media → Facebook**
2. **Enable "Auto-share new entries"**
3. **Set default privacy level** for auto-shared posts
4. **Choose content filters**:
   - Share all entries
   - Share only entries with specific tags
   - Exclude entries with certain tags
5. **Set posting schedule** (optional):
   - Share immediately
   - Daily digest at specific time
   - Weekly summary

#### Deduplication:
- **Personal Diary automatically detects** if you've already shared an entry
- **Prevents duplicate posts** on Facebook
- **Maintains links** between diary entries and Facebook posts
- **Shows sharing status** in entry details

### Managing Your Facebook Connection

#### Viewing Connected Account:
1. **Go to Settings → Social Media**
2. **See your connected Facebook profile**:
   - Profile name and picture
   - Connection date
   - Last successful sync
   - Number of imported posts

#### Reconnecting:
If your Facebook connection expires or has issues:
1. **Tap "Reconnect"** next to your Facebook account
2. **Complete the authorization process** again
3. **Your previous imports remain** unchanged

#### Disconnecting:
1. **Go to Settings → Social Media → Facebook**
2. **Tap "Disconnect"**
3. **Confirm disconnection**
4. **What happens**:
   - Imported entries remain in your diary
   - Future auto-sharing stops
   - Manual sharing becomes unavailable
   - You can reconnect anytime

## 🔒 Privacy and Security

### What Personal Diary Can Access:
- **Your public profile** information (name, profile picture)
- **Your posts** that you choose to import
- **Ability to post** on your behalf (only when you explicitly share)

### What Personal Diary Cannot Access:
- **Private messages** or conversations
- **Posts from friends** (only your own posts)
- **Sensitive account information** (password, financial info)
- **Data from other apps** connected to Facebook

### Data Handling:
- **Imported content** is encrypted using your chosen tier
- **Facebook tokens** are stored securely and encrypted
- **Sharing** only happens when you explicitly choose to share
- **No automatic access** to your Facebook data without permission

### Encryption Tier Considerations:

#### E2E (End-to-End) Users:
- **Imported posts** are encrypted and cannot be read by the server
- **Server-side processing** is limited (no AI features on imported content)
- **Search** of imported content is client-side only
- **Privacy maximum**: Even Personal Diary servers cannot read your imported posts

#### UCE (User-Controlled) Users:
- **Imported posts** can benefit from server-side features
- **AI auto-tagging** works on imported Facebook content
- **Full-text search** includes imported posts
- **Content analysis** for insights and suggestions

## 📋 Best Practices

### Before Connecting:
1. **Review your Facebook privacy settings** first
2. **Consider what you want to import** vs. keep separate
3. **Understand the permanence** of diary vs. social media content
4. **Plan your sharing strategy** in advance

### For Daily Use:
1. **Write in diary first**, then share to social media
2. **Use tags** to control what gets auto-shared
3. **Review content** before sharing to avoid mistakes
4. **Maintain privacy boundaries** - not everything needs to be shared

### Content Strategy:
1. **Private reflections** stay in diary only
2. **Public-appropriate content** can be shared
3. **Use different versions** - diary version vs. social media version
4. **Leverage tags** for content organization and sharing rules

## ⚠️ Important Limitations

### Facebook API Limitations:
- **Import limited** to posts from the last 2 years (Facebook API restriction)
- **Rate limiting** may slow down large imports
- **Deleted Facebook posts** cannot be imported
- **Private messages** are not accessible via API

### Content Limitations:
- **Live videos** are referenced but not downloaded
- **Facebook-specific features** (reactions, comments) are not imported
- **Collaborative posts** with friends may have limited import
- **Event posts** may not include all details

### Technical Limitations:
- **Internet connection required** for all social media features
- **Facebook app permissions** may need occasional renewal
- **Large media files** may take time to import and sync

## 🆘 Troubleshooting Social Media Issues

### Common Problems:

#### "Facebook Connection Failed"
1. **Check internet connection**
2. **Try disconnecting and reconnecting**
3. **Clear app cache** (Android users)
4. **Update the app** to the latest version
5. **Check Facebook is not experiencing outages**

#### "Import Stuck" or "Import Failed"
1. **Check internet connection stability**
2. **Try importing smaller date ranges**
3. **Restart the import process**
4. **Check available storage space**
5. **Contact support** if issue persists

#### "Cannot Share to Facebook"
1. **Verify Facebook connection** is still active
2. **Check if entry contains unsupported content**
3. **Try sharing a different entry** to test
4. **Reconnect your Facebook account**
5. **Check Facebook posting permissions**

### Getting Help:
- **In-app help**: Settings → Help & Support
- **Email support**: support@diary.xmojo.net
- **Community forums**: Discord and Reddit
- **Documentation**: This wiki for detailed guides

## 🔮 Coming Soon: Enhanced Social Features

### Planned Features:
- **Instagram integration** (stories, posts, reels)
- **Twitter/X integration** (tweets, threads, bookmarks)
- **LinkedIn integration** (professional content)
- **Cross-platform analytics** (engagement insights)
- **Smart content suggestions** (what to share where)
- **Scheduling posts** (write now, share later)

### Advanced UCE Features (In Development):
- **AI content optimization** for different platforms
- **Automatic hashtag suggestions**
- **Content performance analytics**
- **Audience analysis** and targeting suggestions

---

**The goal is to make your personal diary the central hub for all your digital content, while maintaining complete control over your privacy and what you choose to share.**