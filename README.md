# LiveConnect Java Chat Widget

Real-time customer support chat for Android — a ready-to-use `Activity`, floating action button, ticket-based sessions, and optional Firebase Cloud Messaging push. Java-friendly API.

---

## What you get

- Full-screen chat `Activity` with customisable theme
- `FloatingChatButton` view with unread badge
- Persistent visitor sessions (ticket-based conversations)
- Image + file attachments
- Optional Firebase Cloud Messaging push notifications

---

## Requirements

| | Minimum |
|---|---|
| Android `minSdk` | 21 (Lollipop) |
| Android `compileSdk` | 34 |
| JDK | 17 |
| Android Gradle Plugin | 8.2.2 |

---

## Install

### 1. Add JitPack to your `settings.gradle.kts`

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add the dependency

In your **app-module** `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.craftindikabiz:live-connect-java-chat-widget:v1.0.3")
}
```

### 3. Enable core library desugaring (**required**)

Still in the app-module `build.gradle.kts`:

```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true   // <-- required
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("com.github.craftindikabiz:live-connect-java-chat-widget:v1.0.3")
}
```

> **Why?** The library uses `ThreadLocal.withInitial` and other Java 8+ APIs that need desugaring on `minSdk < 26`. Without this your app will crash on API 21-25 devices.

---

## Quick start

`MainActivity.java`:

```java
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.techindika.liveconnect.LiveConnectChat;
import com.techindika.liveconnect.model.VisitorProfile;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialise once — typically in MainActivity or Application.
        LiveConnectChat.init(
            this,
            "your-widget-key",
            new VisitorProfile("John Doe", "john@example.com")
        );

        // 2. Open the chat on any click.
        findViewById(R.id.openChatButton)
            .setOnClickListener(v -> LiveConnectChat.show(this));
    }
}
```

That's enough to get a working chat. Details below are opt-in.

---

## Adding a floating chat button

Drop this into any layout — it opens the chat on tap and shows an unread badge automatically:

```xml
<com.techindika.liveconnect.ui.view.FloatingChatButton
    android:id="@+id/chatFab"
    android:layout_width="56dp"
    android:layout_height="56dp"
    android:layout_gravity="bottom|end"
    android:layout_margin="16dp" />
```

No Java code required beyond `LiveConnectChat.init(...)` in `onCreate`.

---

## Theming

Quick: single-colour theme auto-generates the full palette. Pass it as the fourth argument to `init`:

```java
VisitorProfile visitor = new VisitorProfile("John Doe", "john@example.com");
LiveConnectTheme theme = LiveConnectTheme.fromPrimary(Color.parseColor("#4F46E5"));
LiveConnectChat.init(this, "your-widget-key", visitor, theme, null);
```

Detailed: use the builder for fine-grained control.

```java
LiveConnectTheme theme = LiveConnectTheme.builder()
    .primaryColor(Color.parseColor("#4F46E5"))
    .headerTitle("Support")
    .headerBackgroundColor(Color.WHITE)
    .visitorBubbleColor(Color.parseColor("#4F46E5"))
    .agentBubbleColor(Color.parseColor("#F3F4F6"))
    .build();
```

---

## Push notifications (optional)

Chat works without push. Notifications are only needed for background delivery.

**Prerequisite:** a Firebase project with Cloud Messaging enabled, the `google-services.json` file in your `app/` directory, and the `com.google.gms.google-services` plugin applied. See the [Firebase Android setup guide](https://firebase.google.com/docs/android/setup) if you don't have one yet.

Once Firebase is wired up:

```java
// 1. Load the service account JSON from assets (keep it gitignored).
InputStream is = getAssets().open("firebase_service_account.json");
Map<String, Object> serviceAccount = jsonToMap(new JSONObject(readFully(is)));

// 2. Register with the library — service account FIRST, token SECOND.
LiveConnectChat.setFirebaseServiceAccount(serviceAccount);

FirebaseMessaging.getInstance().getToken()
    .addOnSuccessListener(token -> LiveConnectChat.setFcmToken(token));
```

Add the permission to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Request it at runtime on Android 13+:

```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
           != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(
        this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
}
```

> **Important:** `setFirebaseServiceAccount()` must be called **before** `setFcmToken()`. Never commit `firebase_service_account.json` to version control.

---

## API reference

### `LiveConnectChat` — top-level static API

| Method | Purpose |
|---|---|
| `init(context, widgetKey)` | Minimal init — anonymous visitor |
| `init(context, widgetKey, visitorDetails)` | Init with known visitor |
| `init(context, widgetKey, visitorDetails, theme, callback)` | Full init with theme + completion callback |
| `show(context)` | Open the chat `Activity` |
| `setTheme(theme)` | Override the theme at runtime |
| `setFcmToken(token)` | Register the FCM device token |
| `setFirebaseServiceAccount(map)` | Register the Firebase service account for admin push |
| `isInitialized` | Has `init()` been called? |
| `hasCompleteProfile` | Is the visitor profile complete? |

### `VisitorProfile`

| Field | Required | Description |
|---|---|---|
| `name` | yes | Full name |
| `email` | yes | Email address |
| `phone` | no | Phone number (E.164 recommended) |

### `LiveConnectTheme`

Build via `LiveConnectTheme.fromPrimary(int)` for quick theming, or `LiveConnectTheme.builder()` for the full 40+ setters. See `LiveConnectTheme.Builder` in the source jar for the complete list.

### `FloatingChatButton` (view)

Drop-in `com.techindika.liveconnect.ui.view.FloatingChatButton`. Sized 56×56 dp is typical. Auto-tinted from the active theme's primary colour.

---

## Troubleshooting

**`Call LiveConnectChat.init() before LiveConnectChat.show()`**
You're calling `show()` before `init()`. Put the `init` call in `onCreate` (or your `Application.onCreate`) and make sure it runs on every activity entry point.

**Crash on Android 5-7 with `NoSuchMethodError: ThreadLocal.withInitial`**
Core library desugaring is not enabled in the consuming app. Re-read [step 3 of install](#3-enable-core-library-desugaring-required).

**`Could not find com.github.craftindikabiz:live-connect-java-chat-widget`**
The JitPack repository is missing from `settings.gradle.kts`. Add it under `dependencyResolutionManagement.repositories`.

**Push notifications don't arrive**
Order of operations matters: call `setFirebaseServiceAccount(...)` **before** `setFcmToken(...)`. Also verify `google-services.json` is in the `app/` directory and the `com.google.gms.google-services` plugin is applied.

**Stale JitPack build**
JitPack caches per commit SHA. If a tag was moved and you still see old behaviour, open
`https://jitpack.io/#craftindikabiz/live-connect-java-chat-widget/v1.0.3` in a browser and click **Get it** to force a rebuild.

---

## License

UNLICENSED — for internal and licensed partner use only.
