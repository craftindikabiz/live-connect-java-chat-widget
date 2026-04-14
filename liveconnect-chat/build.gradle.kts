plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.techindika.liveconnect"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    // Use real Android resources / SharedPreferences in JVM unit tests via Robolectric.
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Core library desugaring — unlocks Java 8+ APIs (ThreadLocal.withInitial, java.time, etc.) on minSdk 21
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // AndroidX
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Networking (OkHttp only — JSON parsed via org.json.JSONObject)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Socket.IO
    implementation("io.socket:socket.io-client:2.1.1") {
        exclude(group = "org.json", module = "json")
    }

    // Image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Note: Firebase Messaging is NOT a library dependency.
    // Consumer apps add firebase_messaging themselves and pass the FCM token
    // to LiveConnectChat.setFcmToken(). The library only sends the token string
    // to the backend via REST API — no Firebase SDK imports needed.

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ── Unit test dependencies (testImplementation = NOT bundled into the AAR) ──
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    // Real org.json — Android's stub jar is on the test classpath by default
    // and returns null/0/false for everything, breaking JSONObject in unit tests.
    testImplementation("org.json:json:20231013")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            // AGP wires the "release" component automatically because of
            // android.publishing.singleVariant("release") above — no afterEvaluate needed.
            afterEvaluate { from(components["release"]) }

            // JitPack consumers import: com.github.<user>:<repo>:<tag>
            // Version resolution order:
            //  1. -PVERSION_NAME=<tag>   (what JitPack passes on real builds)
            //  2. $VERSION_NAME env var  (alternative JitPack convention)
            //  3. "v1.0.0" fallback      (local publishToMavenLocal testing)
            groupId = "com.github.craftindikabiz"
            artifactId = "live-connect-java-chat-widget"
            version = (project.findProperty("VERSION_NAME") as String?)
                ?: System.getenv("VERSION_NAME")
                ?: "v1.0.0"
        }
    }
}
