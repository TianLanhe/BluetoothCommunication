package com.example.did;

import java.util.Timer;
import java.util.TimerTask;

import com.example.did.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class StartActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);

		// 两秒后跳转到主界面
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				Intent intent = new Intent(getApplicationContext(),
						MainActivity.class);
				startActivity(intent);
				finish();
			}
		}, 2000L);
	}
}
