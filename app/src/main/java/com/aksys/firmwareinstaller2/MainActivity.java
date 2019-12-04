package com.aksys.firmwareinstaller2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
	final String TAG = "GamepadUtility";
	View clickedView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );
	}
	
	public void onClick(View view) {
		clickedView = view;
		if (view.getId() == R.id.detect_gamepad) {
			Intent intent = new Intent( MainActivity.this, GamepadListActivity.class );
			startActivity( intent );
		}
		else if (view.getId() == R.id.about_app) {
			Intent intent = new Intent( MainActivity.this, AboutActivity.class );
			startActivity( intent );
		}
		view.setEnabled(false);
		view.getHandler().postDelayed(new Runnable() {
			@Override
			public void run() {
				clickedView.setEnabled(true);
			}
		}, 500);
	}
//
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		MenuInflater menuInflater = getMenuInflater();
//		menuInflater.inflate( R.menu.menu_gamepadlist, menu );
//		return true;
//	}
//
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		switch ( item.getItemId() ) {
//			case R.id.action_bluetooth:
//				startActivity( new Intent( Settings.ACTION_BLUETOOTH_SETTINGS ) );
//				return true;
//			default:
//				return super.onOptionsItemSelected( item );
//		}
//	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}
