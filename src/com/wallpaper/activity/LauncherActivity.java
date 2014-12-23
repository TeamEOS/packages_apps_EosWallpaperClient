package com.wallpaper.activity;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;

public class LauncherActivity extends Activity {

	@Override
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent intent = new Intent(this, HomeActivity.class);
		super.startActivity(intent);
	}

}
