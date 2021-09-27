package com.aksys.firmwareinstaller2;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class AboutActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_about );
		
		getSupportActionBar().setDisplayHomeAsUpEnabled( true );
		
		TextView version = findViewById( R.id.app_version );
		StringBuilder buffer = new StringBuilder();
		try {
			PackageInfo pInfo = getBaseContext().getPackageManager().getPackageInfo(getPackageName(), 0);
			
			buffer.append(getText(R.string.version)).append(" ").append(pInfo.versionName);
		} catch ( PackageManager.NameNotFoundException e ) {
			buffer.append(getText(R.string.version)).append(" Not Detected.");
		}
		version.setText(buffer.toString());
	}

	@Override
	protected void onStart() {
		super.onStart();
		TextView fileListView = findViewById(R.id.included_file_lists);
		StringBuilder fileNames = new StringBuilder();
		Field[] fields = R.raw.class.getFields();
		for (Field field : fields) {
			String name = field.getName();
			Log.i("Raw Asset: ", name);
			fileNames.append(field.getName()).append("\n");
		}
		fileListView.setText(fileNames.toString());
	}

	//
//	public void onClick(View view) {
//		if (view.getId() == R.id.link_homepage) {
//			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.link_homepage_)));
//			startActivity(intent);
//		}
//		else if (view.getId() == R.id.link_homepage_aks) {
//			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.link_aksys_homepage)));
//			startActivity(intent);
//		}
//	}
}
