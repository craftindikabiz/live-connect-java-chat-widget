package com.techindika.liveconnect.sample

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.techindika.liveconnect.LiveConnectChat
import com.techindika.liveconnect.LiveConnectTheme
import com.techindika.liveconnect.callback.InitCallback
import com.techindika.liveconnect.model.VisitorProfile

/**
 * Sample activity demonstrating LiveConnect SDK usage.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the SDK
        LiveConnectChat.init(
            context = this,
            widgetKey = "your-widget-key-here",
            visitorDetails = VisitorProfile(
                name = "John Doe",
                email = "john@example.com",
                phone = "+14155552671"
            ),
            theme = LiveConnectTheme.fromPrimary(Color.parseColor("#4F46E5")),
            callback = object : InitCallback {
                override fun onSuccess() {
                    // SDK is ready — show UI
                    setContentView(R.layout.activity_main)
                    setupUI()
                }

                override fun onFailure(error: String) {
                    // Still show UI, chat may work with defaults
                    setContentView(R.layout.activity_main)
                    setupUI()
                }
            }
        )
    }

    private fun setupUI() {
        // Open chat from a custom button (like a menu item)
        findViewById<Button>(R.id.openChatButton).setOnClickListener {
            LiveConnectChat.show(this)
        }

        // FloatingChatButton in the layout auto-opens chat on tap
    }
}
