package com.aksys.firmwareinstaller2;

import static com.aksys.firmwareinstaller2.Gamepad.GamepadList.SET_FW_ID;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.aksys.firmwareinstaller2.Gamepad.FirmwareFile;
import com.aksys.firmwareinstaller2.Gamepad.GamepadEvent;
import com.aksys.firmwareinstaller2.Gamepad.GamepadInfo;
import com.aksys.firmwareinstaller2.Gamepad.GamepadList;

import java.util.Objects;

public class FirmwareUpdateActivity extends AppCompatActivity implements GamepadEvent {
	final String TAG = "FWUpdate";
	static final int INTENT_REQUEST = 7025;
	static GamepadInfo gamepad;
	
	Context context;
	Handler handler;
	
	static boolean updating;
	static boolean reinstall;
	FirmwareFile AppFW;
	
	AlertDialog alertDialog;
	ProgressBar bar;
	Button buttonRestart;
	TextView textViewMessage;
	int sendedbyte = 0;
	
	BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Objects.equals(intent.getAction(), BluetoothDevice.ACTION_ACL_CONNECTED)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				SystemClock.sleep(100);
				if (device != null && device.getAddress().startsWith("00:1B:29")) {
					Log.i(TAG, "onReceive: " + intent.getAction() + " / " + device.getName() + " @ STATE: " + device.getBondState());
					gamepad.connectAKSGamepad(device);
					gamepad.onConnectGamepad();
					CheckDeviceAfterRepaired();
				}
			} else if (Objects.equals(intent.getAction(), BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (device != null && Objects.equals(device.getAddress(), gamepad.getAddress())) {
					Log.i(TAG, "onReceive: " + intent.getAction() + " / " + device.getAddress());
					handler.removeCallbacks(PleaseCheckDevice);
				}
			}
		}
	};
	
	int newFirmware = -1;
	boolean needRepair = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_firmware_update );
		context = this;
		textViewMessage = findViewById(R.id.install_message);
		buttonRestart = findViewById(R.id.button_restart_action);
		buttonRestart.setOnClickListener(v -> {
			Log.i(TAG, "onClick: " + buttonRestart.getText() + " from onCreate()");
			finish();
			startActivity(getIntent());
		});
		bar = findViewById(R.id.progressBar);
		
		GamepadList gamepadList = GamepadList.getInstance();
		AppFW = new FirmwareFile(this);
		
		Intent data = getIntent();
		if (data.hasExtra( getString(R.string.intent_gamepad_index) ) && data.getIntExtra( getString(R.string.intent_gamepad_index), -1 ) != -1) {
			gamepad = gamepadList.getIndex( data.getIntExtra( getString(R.string.intent_gamepad_index), -1 ) - 1 );
//			if (gamepad != null) {
//				getSupportActionBar().setTitle( String.format( "%s %s", gamepad.getGamepadName(), getString( R.string.firmware_update ) ) );
//			}
		}
		else if (data.hasExtra( getString(R.string.intent_gamepad_addr) ))
		{
			Log.i(TAG, "find gamepad address: " + data.getStringExtra( getString(R.string.intent_gamepad_addr)));
			gamepad = gamepadList.findGamepadFromBluetoothAddr( data.getStringExtra( getString(R.string.intent_gamepad_addr) ) );
//			if (gamepad != null) {
//				getSupportActionBar().setTitle( String.format( "%s %s", gamepad.getGamepadName(), getString( R.string.firmware_update ) ) );
//			}
//			getSupportActionBar().setTitle( String.format( "%s %s", data.getStringExtra( "Gamepad_Name" ), getString( R.string.menu_sensor ) ) );
		}
		if (data.hasExtra(getString(R.string.intent_reinstall))) {
			reinstall = getIntent().getBooleanExtra(getString(R.string.intent_reinstall), false);
		}
		handler = new Handler();
		CheckDevice();
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		registerReceiver(receiver, filter);
	}
	
	@Override
	public void onBackPressed() {
		Toast.makeText(this, getString(R.string.no_working_cancel_in_firmware), Toast.LENGTH_LONG).show();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		gamepad.setGamepadEvent(null);
		unregisterReceiver(receiver);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent i) {
		super.onActivityResult(requestCode, resultCode, i);
		Log.i(TAG, "onActivityResult: ");
		
		if (requestCode == INTENT_REQUEST && resultCode == Activity.RESULT_OK && i != null) {
			Log.i( TAG, "onActivityResult: " + i.getDataString() );
			if (AppFW.setFile( i.getData() )) {
				CheckStatus();
			}
		}
	}
	
	void CheckDevice() {
		if (gamepad != null) {
			gamepad.setGamepadId();
			gamepad.onServiceHold(true);
			gamepad.setGamepadEvent(this);
			gamepad.onConnectGamepad();
		}
		
		Log.i( TAG, "CheckDevice: updated " + updating );
		if (gamepad != null && !updating && gamepad.getGamepadId() != -1) {
			GetFirmwareFile();
		}
		else if (gamepad != null && updating) {
			// blank Action.
		}
		else {
			SET_FW_ID = -1;
			CancelUpdate("Not Found Gamepad. Please Re-check Gamepad.");
		}
	}
	
	void CheckDeviceAfterRepaired() {
		if (updating) {
			gamepad.setGamepadEvent(this);
			Log.i( TAG, "CheckDeviceAfterRepaired" );
			
			handler.post(new Runnable() {
				@Override
				public void run() {
					buttonRestart.setText("RECONNECT");
					buttonRestart.setOnClickListener(v -> {
						Log.i(TAG, "onClick: " + buttonRestart.getText() + " from CheckDeviceAfterRepaired()");
						gamepad.onDisconnectGamepad();
						SystemClock.sleep(100);
					});
					buttonRestart.setVisibility(View.VISIBLE);
					
					textViewMessage.setText(R.string.text_recheck_gamepad_status);
				}
			});
//			handler.postDelayed(PleaseCheckDevice,10000);
		}
	}
	
	Runnable PleaseCheckDevice = new Runnable() {
		@Override
		public void run() {
			gamepad.onConnectGamepad();
			gamepad.CheckFirmware();
			handler.postDelayed(this, 1000);
		}
	};
	
//	void OpenFirmwareDialog() {
//		AlertDialog.Builder builder = new AlertDialog.Builder( this );
//		builder.setIcon( R.drawable.ic_download );
//		builder.setTitle( "Firmware Update" );
//		builder.setMessage( "Do you want install new Firmware?" );
//		builder.setPositiveButton( android.R.string.ok, new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				GetFirmwareFile();
//			}
//		} );
//		builder.setNegativeButton( android.R.string.cancel, new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				CancelUpdate();
//			}
//		} );
//
//		alertDialog = builder.create();
//		alertDialog.show();
//	}
	
	void GetFirmwareFile() {
		if (alertDialog != null) {
			alertDialog.dismiss();
			alertDialog = null;
		}
		if (getIntent().hasExtra("fw_file")) {
			int file = getIntent().getIntExtra("fw_file", -1);
			Log.i(TAG, "GetFirmwareFile: " + file);
			if (file != -1 && AppFW.setFile(file)) CheckStatus();
			else if (getIntent().hasExtra("fw_path") && AppFW.setFile(getIntent().getStringExtra("fw_path")))
				CheckStatus();
			else {
				// Not Detect Firmware.
				Log.w(TAG, "GetFirmwareFile: Not Detect Firmware.");
				CancelUpdate("Not Found target firmware file.");
			}
		} else {
			Intent i = new Intent( Intent.ACTION_OPEN_DOCUMENT );
			i.addCategory( Intent.CATEGORY_OPENABLE );
			i.setType( "*/*" );
			startActivityForResult( i, INTENT_REQUEST );
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration configuration ) {
		super.onConfigurationChanged( configuration );
		Log.i( TAG, "onConfigurationChanged\n" + configuration.toString() );
		
//		if (gamepad.getBondStatus() >= GamepadInfo.STATUS_WRITING) updating = true;
		if (!updating) {
			gamepad.onConnectGamepad();
			SystemClock.sleep(100);
			gamepad.UpdateComplete();
			SystemClock.sleep(20);
			gamepad.CheckFirmware();
			CheckDevice();
		} else {
			//// Maybe Gamepad Re-Connected.
		}
		if (gamepad != null) gamepad.setGamepadId();
	}
	
	private void CheckStatus() {
		AlertDialog.Builder builder = new AlertDialog.Builder( this );
		if (AppFW.getFirmwareVersion() == 0) {
			/// File Error.
			builder.setIcon( R.drawable.ic_download );
			builder.setTitle( R.string.firmware_update );
			builder.setMessage( getString(R.string.alert_file_notfound));
			builder.setPositiveButton( android.R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					CancelUpdate("selected Cancel because not found firmware");
				}
			} );
			builder.setNegativeButton( R.string.text_search_file, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					GetFirmware();
				}
			} );
		}
		else if (!updating) {
			/// File is Correct
			builder.setIcon( R.drawable.ic_download );
			needRepair = true;
			
			builder.setTitle(R.string.firmware_update);
			builder.setMessage(String.format(getString(R.string.question_firmware_install), String.valueOf(AppFW.getFirmwareVersion()), gamepad.getGamepadName()));
			
			builder.setCancelable(false);
			
			builder.setPositiveButton( android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					InstallFirmware();
				}
			} );
			builder.setNegativeButton( android.R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					SET_FW_ID = -1;
					dialog.dismiss();
					CancelUpdate(String.format("not want to install firmware.\nVersion: %d\nDevice: %s", AppFW.getFirmwareVersion(), gamepad.getGamepadName()));
				}
			} );
		}
		alertDialog = builder.create();
		alertDialog.show();
	}
	
	void InstallFirmware() {
		updating = true;
		if (alertDialog != null) {
			alertDialog.dismiss();
			alertDialog = null;
		}
		gamepad.TryFirmwareWrite( AppFW.getFileByteArray() );
	}
	
	void CancelUpdate(String reasons) {
		CancelUpdate(reasons, "Install Cancelled.");
	}
	
	void CancelUpdate(String reasons, String title) {
		Log.i(TAG, "CancelUpdate");
		if (alertDialog != null) alertDialog.dismiss();
		gamepad.onDisconnectGamepad();
		AlertDialog.Builder builder = new AlertDialog.Builder( this );
		builder.setIcon( R.drawable.ic_download );
		builder.setTitle(title);
		builder.setMessage(String.format("Firmware install is cancelled for the following reasons:\n%s", reasons));
		builder.setPositiveButton("CLOSE", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				updating = false;
				finish();
			}
		});
		alertDialog = builder.create();
		alertDialog.show();
	}
	
	void FinishUpdate() {
		Log.i(TAG, "FinishUpdate");
		handler.removeCallbacks(PleaseCheckDevice);
		
		handler.post(() -> {
			ImageView image = findViewById(R.id.imageView3);
			image.setImageResource(R.drawable.ic_gamepad_check);
			bar.setIndeterminate(false);
			bar.setProgress(bar.getMax());
			textViewMessage.setText(String.format(getString(R.string.firmware_update_completed_desc), gamepad.getGamepadName(), gamepad.getFirmware()));

			buttonRestart.setVisibility(View.VISIBLE);
			Button button = findViewById(R.id.button_close_action);
			button.setOnClickListener(v -> CloseActivityAndRemove());
			button.setVisibility(View.VISIBLE);
			Button buttonClear = findViewById(R.id.button_clear_action);
			buttonClear.setOnClickListener(v -> CloseActivityAndClearGamepad());
			if(BuildConfig.DEBUG) buttonClear.setVisibility(View.VISIBLE);
		});
	}
	
	void CloseActivityAndRemove() {
		Log.i(TAG, "CloseActivityAndRemove: ");
		if (alertDialog != null) {
			alertDialog.dismiss();
		}
		gamepad.setGamepadEvent(null);
		gamepad.onDisconnectGamepad();
		gamepad.removePair();
		updating = false;
		finish();
	}
	
	void CloseActivityAndClearGamepad() {
		Log.i(TAG, "CloseActivityAndClearGamepad: ");
		if (alertDialog != null) {
			alertDialog.dismiss();
		}
		gamepad.SendByte((byte) 0xFB);
		SystemClock.sleep(50);
		gamepad.setGamepadEvent(null);
		gamepad.onDisconnectGamepad();
		gamepad.removePair();
		updating = false;
		finish();
	}
	
	@Override
	public void ConnectionFail() {
		if (updating) {
			/// updated.
			Log.i(TAG, "Cannot Connected or rebooting...");
			handler.post(() -> {
				if (!bar.isIndeterminate() && bar.getProgress() <= bar.getMax() && (alertDialog == null || !alertDialog.isShowing())) {
					gamepad.onDisconnectGamepad();
					gamepad.removePair();
					CancelUpdate("Gamepad is Disconnected!\nPlease Reset this gamepad and repair gamepad.", "WARNING!");
					GamepadList.getInstance().checkList(this);
				}
			});
		}
		else {
			/// Connection Fail.
			gamepad.onDisconnectGamepad();
			handler.post(() -> textViewMessage.setText("Please Reboot Gamepad and Press 'RESTART'."));
		}
	}
	
	@Override
	public void ConnectionSuccess() {
		if (updating) {
			Log.i(TAG, "Upgrade Success Checked.");
			GetFirmware();
		}
	}
	
	@Override
	public void GetFirmware() {
		if (updating) {
			Log.i(TAG, "Firmware Value Detected. " + gamepad.getFirmware() + " target: " + AppFW.getFirmwareVersion());
			if (gamepad.getFirmware().equals("1") || gamepad.getFirmwareInt() == -1) {
				gamepad.CheckFirmware();
			} else if (gamepad.getFirmwareInt() == AppFW.getFirmwareVersion()) {
				FinishUpdate();
			} else {
				FinishUpdate();
				runOnUiThread(() -> {
					Button buttonClear = findViewById(R.id.button_clear_action);
					buttonClear.setText("RE-INSTALL");
					buttonClear.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							updating = false;
							InstallFirmware();
						}
					});
				});
			}
		}
	}
	
	@Override
	public void GetBattery() {
		final float battery = gamepad.getBattery();
		if (updating) {
			Log.i(TAG, "Battery Value Detected. " + battery);
		}
		else if (battery < 5000) {
			handler.post(() -> {
				bar.setIndeterminate(false);
				bar.setProgress(0);
				textViewMessage.setText(R.string.text_firmware_need_power);
				buttonRestart.setOnClickListener(v -> {
					Log.i(TAG, "onClick: " + buttonRestart.getText() + " from GetBattery()");
					InstallFirmware();
					findViewById(R.id.button_restart_action).setVisibility(View.GONE);
				});
				buttonRestart.setVisibility(View.VISIBLE);
			});
		}
		else if (battery > 5000) {
			InstallFirmware();
		}
//		else if (AppFW.getFilebyteArray() != null) {
//			InstallFirmware();
//		}
	}
	
	@Override
	public void SendingFirmware(final int sendbytes) {
		handler.post(() -> {
			sendedbyte += sendbytes;
			bar.setIndeterminate(false);
			bar.setMax(AppFW.getFileSize());
			bar.setProgress(sendedbyte);
			textViewMessage.setText(getString(R.string.text_now_installing) + getString(R.string.text_now_firmware_installing));
		});
	}
	
	@Override
	public void GetDeviceCode() {
	
	}
	
	@Override
	public void FirmwareInstalled() {
		Log.i(TAG, "FirmwareInstalled");
		handler.post(() -> {
			bar.setIndeterminate(true);
			textViewMessage.setText(R.string.text_gamepad_is_rebooting);
			buttonRestart.setText("RECONNECT");
			buttonRestart.setOnClickListener(v -> {
				Log.i(TAG, "onClick: " + buttonRestart.getText() + " from FirmwareInstalled()");
				gamepad.onDisconnectGamepad();
				SystemClock.sleep(100);
				gamepad.onConnectGamepad();
				CheckDeviceAfterRepaired();
			});
			buttonRestart.setVisibility(View.VISIBLE);
		});
	}
	
	@Override
	public void FirmwareFailed() {
		Log.i(TAG, "Firmware Failed");
		handler.post(() -> {
			bar.setIndeterminate(false);
			bar.setProgress(0);
			textViewMessage.setText(R.string.text_firmware_need_power);
			buttonRestart.setOnClickListener(v -> {
				Log.i(TAG, "onClick: " + buttonRestart.getText() + " from GetBattery()");
				InstallFirmware();
				findViewById(R.id.button_restart_action).setVisibility(View.GONE);
			});
			buttonRestart.setVisibility(View.VISIBLE);
		});
	}
}
