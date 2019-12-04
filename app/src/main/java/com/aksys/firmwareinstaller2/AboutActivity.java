package com.aksys.firmwareinstaller2;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {
	StringBuffer buffer = new StringBuffer();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_about );
		
		getSupportActionBar().setDisplayHomeAsUpEnabled( true );
		
		TextView version = findViewById( R.id.app_version );
		try {
			PackageInfo pInfo = getBaseContext().getPackageManager().getPackageInfo(getPackageName(), 0);
			
			buffer.append(getText( R.string.version ) + " "  + pInfo.versionName);
		} catch ( PackageManager.NameNotFoundException e ) {
			buffer.append( getText( R.string.version ) + " Not Detected.");
		}
		version.setText(buffer.toString());
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
