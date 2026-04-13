package com.techindika.liveconnect.sample;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.techindika.liveconnect.LiveConnectChat;
import com.techindika.liveconnect.LiveConnectTheme;
import com.techindika.liveconnect.callback.InitCallback;
import com.techindika.liveconnect.model.VisitorProfile;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        VisitorProfile visitor = new VisitorProfile(
                "John Doe",
                "john@example.com",
                "+14155552671"
        );

        LiveConnectTheme theme = LiveConnectTheme.fromPrimary(Color.parseColor("#4F46E5"));

        LiveConnectChat.init(
                this,
                "your-widget-key-here",
                visitor,
                theme,
                new InitCallback() {
                    @Override
                    public void onSuccess() {
                        setContentView(R.layout.activity_main);
                        setupUI();
                    }

                    @Override
                    public void onFailure(String error) {
                        setContentView(R.layout.activity_main);
                        setupUI();
                    }
                }
        );
    }

    private void setupUI() {
        Button openChatButton = findViewById(R.id.openChatButton);
        openChatButton.setOnClickListener(v -> LiveConnectChat.show(this));
    }
}
