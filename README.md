# LiveConnect Java Chat Widget

A production-ready Android chat widget for real-time customer support with ticket-based conversations, push notifications, and persistent sessions.

This package is designed for internal/private use, enabling seamless integration of live chat functionality into Java Android applications.

---

## Table of Contents

1. [Features](#features)
2. [Requirements](#requirements)
3. [Installation](#installation)
4. [Package Initialization](#package-initialization)
5. [Adding the Chat Button (FAB)](#adding-the-chat-button-fab)
6. [Opening Chat Programmatically](#opening-chat-programmatically)
7. [Theme Customization](#theme-customization)
8. [Firebase Project Setup](#firebase-project-setup)
9. [Push Notification Configuration](#push-notification-configuration)
10. [Full MainActivity.java Example](#full-mainactivityjava-example)
11. [How It Works](#how-it-works)
12. [API Reference](#api-reference)
13. [Security Guidelines](#security-guidelines)
14. [Platform Support](#platform-support)
15. [Troubleshooting](#troubleshooting)
16. [License](#license)

---

## Features

- Real-time chat messaging using Socket.IO
- Ticket-based conversation system
- Session persistence using SharedPreferences
- Push notifications via Firebase Cloud Messaging (FCM)
- Optional file attachments (images and documents)
- Theme customization (100+ properties)
- Unread message badge
- Conversation history (Activity tab)
- Lightweight and efficient

---

## Requirements

| Requirement       | Minimum Version         |
|-------------------|-------------------------|
| Android API Level | 21+ (Lollipop)          |
| Java              | 17+                     |
| Gradle            | 8.0+                    |
| Firebase Project  | Required for FCM        |

---

## Installation

### Step 1 — Add JitPack Repository

Since the package is hosted on GitHub and not published to Maven Central, add JitPack as a repository.

**settings.gradle (Groovy):**

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**Or settings.gradle.kts (Kotlin DSL):**

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2 — Add Dependency

In your app-level `build.gradle`:

```groovy
dependencies {
    implementation 'com.github.craftindikabiz:live-connect-java-chat-widget:v1.0.0'
    implementation 'com.google.firebase:firebase-messaging:23.4.0'
}
```

### Step 3 — Sync

```bash
./gradlew sync
```

> **Warning — JitPack build cache.** JitPack caches built artifacts per commit SHA. If a build was already triggered on an older commit of a tag that has since been moved, subsequent installs may receive a stale artifact. To force a rebuild, open the version page in a browser and click **Get it** on the affected version:
>
> ```
> https://jitpack.io/#craftindikabiz/live-connect-java-chat-widget/v1.0.0
> ```
>
> JitPack will re-resolve the tag to the latest SHA and rebuild. If you need to switch to a new commit without moving the tag, pin to a fresh version instead.

---

## Package Initialization

Initialize the package in your `MainActivity.java` or `Application` class. This is mandatory. The widget will not function correctly if initialization is deferred.

```java
import com.techindika.liveconnect.LiveConnectChat;
import com.techindika.liveconnect.model.VisitorProfile;
import com.techindika.liveconnect.callback.InitCallback;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LiveConnectChat.init(
            this,
            "your-widget-key",
            new VisitorProfile("John Doe", "john@example.com", "+14155552671"),
            null,  // theme (null for default)
            new InitCallback() {
                @Override
                public void onSuccess() {
                    // SDK is ready
                }

                @Override
                public void onFailure(String error) {
                    // Handle error
                }
            }
        );

        setContentView(R.layout.activity_main);
    }
}
```

**Parameters:**

| Parameter        | Type               | Required | Description                                      |
|------------------|--------------------|----------|--------------------------------------------------|
| `context`        | `Context`          | Yes      | Application or Activity context                  |
| `widgetKey`      | `String`           | Yes      | Unique key provided by the LiveConnect dashboard |
| `visitorDetails` | `VisitorProfile`   | No       | Identifies the end user in chat sessions         |
| `theme`          | `LiveConnectTheme` | No       | Overrides the default visual theme               |
| `callback`       | `InitCallback`     | No       | Called when initialization completes or fails     |

**VisitorProfile fields:**

| Field   | Type     | Required | Description                             |
|---------|----------|----------|-----------------------------------------|
| `name`  | `String` | Yes      | Full name of the visitor                |
| `email` | `String` | Yes      | Email address                           |
| `phone` | `String` | No       | Phone number (E.164 format recommended) |

---

## Adding the Chat Button (FAB)

Use `FloatingChatButton` in your layout XML. The button automatically opens the chat screen on tap and shows an unread message badge.

```xml
<!-- In your activity_main.xml -->
<com.techindika.liveconnect.ui.view.FloatingChatButton
    android:id="@+id/chatFab"
    android:layout_width="56dp"
    android:layout_height="56dp"
    android:layout_gravity="bottom|end"
    android:layout_margin="16dp" />
```

No additional Java code is needed. The FAB handles click-to-open and unread badge automatically.

---

## Opening Chat Programmatically

To open the chat screen directly from any widget or user interaction, call:

```java
LiveConnectChat.show(context);
```

This method can be used independently without the floating action button (FAB). It is useful for triggering the chat from a custom button, a menu item, a notification tap, or any other event in your application.

### Example: Opening Chat from a Custom Button

```java
Button supportButton = findViewById(R.id.supportButton);
supportButton.setOnClickListener(v -> {
    LiveConnectChat.show(this);
});
```

### Example: Opening Chat from a Navigation Drawer

```java
navigationView.setNavigationItemSelectedListener(menuItem -> {
    if (menuItem.getItemId() == R.id.nav_chat) {
        drawerLayout.closeDrawers();
        LiveConnectChat.show(this);
        return true;
    }
    return false;
});
```

### Example: Using FAB and Programmatic Show Together

Both the floating action button and `LiveConnectChat.show()` can coexist in your application. Use the FAB for permanent access and `LiveConnectChat.show()` for contextual triggers.

```java
public class MyApp extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Open chat from toolbar help button
        ImageButton helpButton = findViewById(R.id.helpButton);
        helpButton.setOnClickListener(v -> {
            LiveConnectChat.show(this);
        });

        // FloatingChatButton in layout handles itself automatically
    }
}
```

---

## Theme Customization

A custom theme can be applied at initialization time using the `theme` parameter of `LiveConnectChat.init()`.

### Quick Setup (Single Color)

```java
LiveConnectChat.init(
    this,
    "your-widget-key",
    new VisitorProfile("John Doe", "john@example.com", "+14155552671"),
    LiveConnectTheme.fromPrimary(Color.parseColor("#4F46E5")),
    null
);
```

### Detailed Customization (Builder Pattern)

```java
LiveConnectTheme theme = LiveConnectTheme.builder()
    .primaryColor(Color.BLUE)
    .headerBackgroundColor(Color.WHITE)
    .headerTitle("Support Chat")
    .headerTitleColor(Color.DKGRAY)
    .visitorBubbleColor(Color.BLUE)
    .agentBubbleColor(Color.LTGRAY)
    .build();

LiveConnectChat.init(this, "your-widget-key", null, theme, null);
```

The `LiveConnectTheme` class accepts 100+ visual properties including colors, font sizes, border radii, and spacing. The `fromPrimary()` factory auto-derives all sub-colors from a single brand color.

---

## Firebase Project Setup

Push notifications require a configured Firebase project. Follow the steps below carefully.

### Step 1 — Create or Select a Firebase Project

1. Open the Firebase Console: https://console.firebase.google.com
2. Create a new project or open an existing one.
3. Navigate to **Project Settings** and ensure **Cloud Messaging** is enabled under the **Cloud Messaging** tab.

### Step 2 — Add Your Android App to Firebase

1. In the Firebase Console, click **Add app** and select Android.
2. Enter your app's package name (found in `build.gradle` under `applicationId`).
3. Download the generated `google-services.json` file.
4. Place the file at:

```
app/google-services.json
```

5. In your root `build.gradle`, add the Google services plugin:

```groovy
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.4.0'
    }
}
```

6. In your app-level `build.gradle`, apply the plugin at the bottom:

```groovy
apply plugin: 'com.google.gms.google-services'
```

### Step 3 — Add Platform Permissions

Add the following to `AndroidManifest.xml` inside the `<manifest>` tag:

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

## Push Notification Configuration

### Step 4 — Download and Add the Firebase Service Account

1. In the Firebase Console, go to **Project Settings** → **Service Accounts**.
2. Click **Generate New Private Key**.
3. Save the downloaded file as `firebase_service_account.json`.
4. Place the file at:

```
app/src/main/assets/firebase_service_account.json
```

5. Add the file to `.gitignore`:

```
# Firebase Service Account — do not commit
**/assets/firebase_service_account.json
```

**IMPORTANT SECURITY NOTE:** Never hardcode private keys or service account credentials directly in your code. Always load from a secure file in assets.

### Step 5 — Load the Service Account and Register the FCM Token

The order of operations is critical — `setFirebaseServiceAccount()` must be called before `setFcmToken()`.

```java
import org.json.JSONObject;
import com.google.firebase.messaging.FirebaseMessaging;
import com.techindika.liveconnect.LiveConnectChat;

// Load Firebase service account from assets
InputStream is = getAssets().open("firebase_service_account.json");
BufferedReader reader = new BufferedReader(new InputStreamReader(is));
StringBuilder sb = new StringBuilder();
String line;
while ((line = reader.readLine()) != null) {
    sb.append(line);
}
reader.close();
JSONObject json = new JSONObject(sb.toString());
Map<String, Object> serviceAccount = toMap(json);

// Set the service account (required before setting the FCM token)
LiveConnectChat.setFirebaseServiceAccount(serviceAccount);

// Get and register the FCM device token
FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
    LiveConnectChat.setFcmToken(token);
});
```

### Step 6 — Request Notification Permission (Android 13+)

```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            this,
            new String[]{Manifest.permission.POST_NOTIFICATIONS},
            1001
        );
    }
}
```

---

## Full MainActivity.java Example

The following is a complete, production-oriented `MainActivity.java` that combines all steps:

```java
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessaging;
import com.techindika.liveconnect.LiveConnectChat;
import com.techindika.liveconnect.LiveConnectTheme;
import com.techindika.liveconnect.callback.InitCallback;
import com.techindika.liveconnect.model.VisitorProfile;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load Firebase service account from assets
        Map<String, Object> serviceAccount = loadServiceAccount();

        // Initialize the chat widget
        LiveConnectChat.init(
            this,
            "your-widget-key",
            new VisitorProfile("John Doe", "john@example.com", "+14155552671"),
            LiveConnectTheme.fromPrimary(Color.parseColor("#4F46E5")),
            new InitCallback() {
                @Override
                public void onSuccess() {
                    // SDK is ready
                }

                @Override
                public void onFailure(String error) {
                    // Handle initialization error
                }
            }
        );

        // Set Firebase credentials and FCM token
        if (serviceAccount != null) {
            LiveConnectChat.setFirebaseServiceAccount(serviceAccount);
        }

        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            LiveConnectChat.setFcmToken(token);
        });

        // Request notification permission (Android 13+)
        requestNotificationPermission();

        setContentView(R.layout.activity_main);

        // Open chat from a custom button
        Button supportButton = findViewById(R.id.supportButton);
        supportButton.setOnClickListener(v -> {
            LiveConnectChat.show(this);
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    1001
                );
            }
        }
    }

    private Map<String, Object> loadServiceAccount() {
        try {
            InputStream is = getAssets().open("firebase_service_account.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject json = new JSONObject(sb.toString());
            return jsonToMap(json);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> jsonToMap(JSONObject json) {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            map.put(key, json.opt(key));
        }
        return map;
    }
}
```

---

## How It Works

The widget uses a ticket-based conversation system:

1. When a user sends their first message, a ticket is created on the server
2. The active ticket ID is stored locally using SharedPreferences
3. On app restart, the previous ticket is automatically resumed
4. Socket.IO maintains a persistent connection for real-time messaging
5. FCM tokens receive push notifications even when the app is in the background

---

## API Reference

### Methods

| Method                                        | Description                                                                          | Parameters                                                          |
|-----------------------------------------------|--------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| `LiveConnectChat.init()`                      | Initializes the chat widget. Must be called before `show()`.                         | `context`, `widgetKey`, `visitorDetails`, `theme`, `callback`       |
| `LiveConnectChat.show()`                      | Opens the chat screen programmatically.                                              | `context`                                                           |
| `LiveConnectChat.setFirebaseServiceAccount()` | Passes Firebase service account credentials. Must be called before `setFcmToken()`. | `serviceAccount` (Map<String, Object>)                              |
| `LiveConnectChat.setFcmToken()`               | Registers the device FCM token for push notifications.                               | `fcmToken` (String)                                                 |
| `LiveConnectChat.setTheme()`                  | Overrides the theme at runtime.                                                      | `theme` (LiveConnectTheme)                                          |

### Widgets

| Widget                      | Description                                                                |
|-----------------------------|----------------------------------------------------------------------------|
| `FloatingChatButton`        | A pre-built floating action button that opens the chat screen on tap.      |

### VisitorProfile

```java
new VisitorProfile(
    "John Doe",           // name (required)
    "john@example.com",   // email (required)
    "+14155552671"         // phone (optional, pass "" if not available)
)
```

---

## Security Guidelines

The Firebase service account key grants privileged access to your Firebase project. Handle it with the same care as a production password.

- Load the service account exclusively from `assets/firebase_service_account.json`, never inline it as a string in code.
- Add `firebase_service_account.json` to `.gitignore` before the first commit.
- Never share the service account file via email, Slack, or public channels.
- Do not log or print any fields from the service account object.
- Rotate the service account key if you suspect it has been exposed.

---

## Platform Support

| Platform          | Supported |
|-------------------|-----------|
| Android (API 21+) | Yes       |

---

## Troubleshooting

### Chat Widget Not Showing

1. Confirm `LiveConnectChat.init()` is called before `show()`
2. Verify the `widgetKey` is valid and matches the one in your LiveConnect dashboard
3. Ensure the device has an active internet connection
4. Check that `INTERNET` permission is declared in `AndroidManifest.xml`

### FCM Token Not Available

1. Confirm `google-services.json` is placed in `app/`
2. Test on a physical device — FCM tokens are unreliable on emulators
3. Ensure the app has internet access

### Notifications Not Received

1. Confirm `setFirebaseServiceAccount()` is called before `setFcmToken()`
2. Confirm `setFcmToken()` is called with a non-null, valid token
3. Verify Cloud Messaging is enabled in the Firebase project settings
4. On Android 13+, confirm the notification runtime permission has been granted
5. Verify the Firebase service account has the Firebase Cloud Messaging Admin role in Google Cloud IAM
6. Confirm the device has an active internet connection

### Messages Not Appearing in Chat

1. Check internet connectivity
2. Verify the Socket.IO connection is active (review logcat for `LiveConnect` tag)
3. Confirm the LiveConnect backend server is reachable
4. Review logcat output for any error messages from the package

---

## License

This package is private and intended for internal use only.

**Status:** Production Ready
**Version:** 1.0.0
**Last Updated:** April 10, 2026
