package com.aksys.firmwareinstaller2;

import static com.aksys.firmwareinstaller2.Gamepad.GamepadList.checkBluetoothPermission;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
	final String TAG = "GamepadUtility";
	View clickedView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );
		TextView view = findViewById(R.id.application_version);

		//// 업데이트 날짜를 기재 하십시오.
		view.setText(String.format("%s %s", BuildConfig.VERSION_NAME, "2024-01-18"));
	}

	ActivityResultLauncher<String> getPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
		if (result) {
			Intent intent = new Intent(MainActivity.this, GamepadListActivity.class);
			startActivity(intent);
		}
	});

	public void onClick(View view) {
		clickedView = view;
		if (view.getId() == R.id.detect_gamepad) {
			if (checkBluetoothPermission(this)) {
				Intent intent = new Intent(MainActivity.this, GamepadListActivity.class);
				startActivity(intent);
			} else {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
					getPermission.launch(Manifest.permission.BLUETOOTH_CONNECT);
				}
			}
		}
		else if (view.getId() == R.id.about_app) {
			Intent intent = new Intent( MainActivity.this, AboutActivity.class );
			startActivity( intent );
		}
		view.setEnabled(false);
		view.getHandler().postDelayed(() -> clickedView.setEnabled(true), 500);
	}

//	private ActivityResultRegistry bluetoothCallback = new ActivityResultRegistry() {
//		@Override
//		public <I, O> void onLaunch(int requestCode, @NonNull ActivityResultContract<I, O> contract, I input, @Nullable ActivityOptionsCompat options) {
//
//		}
//	};
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
