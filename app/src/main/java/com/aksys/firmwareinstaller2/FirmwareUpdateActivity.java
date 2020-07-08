package com.aksys.firmwareinstaller2;

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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.aksys.firmwareinstaller2.Gamepad.CheckBluetooth;
import com.aksys.firmwareinstaller2.Gamepad.FirmwareFile;
import com.aksys.firmwareinstaller2.Gamepad.GamepadEvent;
import com.aksys.firmwareinstaller2.Gamepad.GamepadInfo;
import com.aksys.firmwareinstaller2.Gamepad.GamepadList;

import static com.aksys.firmwareinstaller2.Gamepad.GamepadInfo.TYPE_AKS_BT;
import static com.aksys.firmwareinstaller2.Gamepad.GamepadList.SET_FW_ID;


public class FirmwareUpdateActivity extends AppCompatActivity implements GamepadEvent {
	final String TAG = "FWUpdate";
	static final int INTENT_REQUEST = 7025;
	static final int INTENT_UPDATE_COMPLETE = 4765;
	static final int INTENT_UPDATE_FAILURE = 4766;
	static GamepadInfo gamepad;
	
	Context context;
	Handler handler;
	
	static boolean updating;
	static boolean reinstall;
	FirmwareFile AppFW;
	
	AlertDialog alertDialog;
	ProgressBar bar;
	TextView textViewMessage;
	int sendedbyte = 0;
	
	BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction() == BluetoothDevice.ACTION_ACL_CONNECTED) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Intent i = CheckBluetooth.CheckBluetoothDevice(device.getName());
				if (i.getIntExtra( "TYPE", 0 ) == TYPE_AKS_BT ) {
					Log.i(TAG, "onReceive: " + intent.getAction() + " / " + device.getName());
					CheckDeviceAfterRepaired();
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
	
	//
//	@Override
//	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		Log.i( TAG, "onActivityResult: " );
//		int file = data.getIntExtra("fw_file", -1);
//		if (file == -1) {
//			// TODO: Not Found file.
//			Log.e(TAG, "onActivityResult: Not Found FW File." );
//		}
//		if ( requestCode == INTENT_REQUEST && data != null ) {
//			Log.i( TAG, "onActivityResult: file - " + file );
//			AppFW = new FirmwareFile(this);
//			if ( AppFW.setFile( file ) ) {
//				CheckStatus();
//			}
//			return;
//		}
//	}
	
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
		if (gamepad != null && updating && gamepad.getGamepadId() == -1) {
			Log.i( TAG, "CheckDeviceAfterRepaired" );
			gamepad.setGamepadEvent(this);
			handler.post(new Runnable() {
				@Override
				public void run() {
					textViewMessage.setText(R.string.text_recheck_gamepad_status);
				}
			});
			gamepad.onConnectGamepad();
			handler.postDelayed(PleaseCheckDevice,1000);
		}
	}
	
	Runnable PleaseCheckDevice = new Runnable() {
		@Override
		public void run() {
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
		int file = getIntent().getIntExtra("fw_file", -1);
		Log.i(TAG, "GetFirmwareFile: " + file + " / name: " );
		if (file != -1 && AppFW.setFile(file)) CheckStatus();
		else if (getIntent().hasExtra("fw_path") && AppFW.setFile(getIntent().getStringExtra("fw_path"))) CheckStatus();
		else {
			// Not Detect Firmware.
			Log.w(TAG, "GetFirmwareFile: Not Detect Firmware.");
			CancelUpdate("Not Found target firmware file.");
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
					updating = true;
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
		if (alertDialog != null) {
			alertDialog.dismiss();
			alertDialog = null;
		}
		gamepad.TryFirmwareWrite( AppFW.getFilebyteArray() );
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
				finish();
			}
		});
		alertDialog = builder.create();
		alertDialog.show();
	}
	
	void FinishUpdate() {
		Log.i(TAG, "FinishUpdate");
		handler.removeCallbacks(PleaseCheckDevice);
		
		handler.post(new Runnable() {
			@Override
			public void run() {
				bar.setIndeterminate(false);
				bar.setProgress(bar.getMax());
				textViewMessage.setText(String.format(getString(R.string.firmware_update_completed_desc), gamepad.getGamepadName(), gamepad.getFirmware()));
				Button button = findViewById(R.id.button_close_action);
				button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						CloseActivityAndRemove();
					}
				});
				button.setVisibility(View.VISIBLE);
			}
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
		finish();
	}
	
	@Override
	public void ConnectionFail() {
		if (updating) {
			/// updated.
			handler.post(new Runnable() {
				@Override
				public void run() {
					if (!bar.isIndeterminate() && bar.getProgress() <= bar.getMax() && (alertDialog == null || !alertDialog.isShowing())) {
						gamepad.onDisconnectGamepad();
						gamepad.removePair();
						CancelUpdate("Gamepad is Disconnected!\nPlease Reset this gamepad and repair gamepad.", "WARNING!");
						GamepadList.getInstance().checkList();
					}
				}
			});
		}
		else {
			/// Connection Fail.
			gamepad.onDisconnectGamepad();
			gamepad.removePair();
			GamepadList.getInstance().checkList();
			CancelUpdate(getString(R.string.text_gamepad_notfound_desc));
		}
	}
	
	@Override
	public void ConnectionSuccess() {
		if (updating) {
			Log.i(TAG, "Upgrade Success Checked.");
			GetFirmware();
		} else {
		
		}
	}
	
	@Override
	public void GetFirmware() {
		if (updating) {
			Log.i(TAG, "Firmware Value Detected. " + gamepad.getFirmware() + " target: " + AppFW.getFirmwareVersion());
			if (gamepad.getFirmware() == "1" || gamepad.getFirmwareInt() == -1) {
				gamepad.CheckFirmware();
			} else {
				FinishUpdate();
			}
		}
	}
	
	@Override
	public void GetBattery() {
		if (updating) {
			Log.i(TAG, "Battery Value Detected. " + gamepad.getBattery());
		}
	}
	
	@Override
	public void SendingFirmware(final int sendbytes) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				sendedbyte += sendbytes;
				bar.setIndeterminate(false);
				bar.setMax(AppFW.getFilesize());
				bar.setProgress(sendedbyte);
				textViewMessage.setText(String.format(getString(R.string.text_now_installing), (float)(sendedbyte * 100 /AppFW.getFilesize())) + getString(R.string.text_now_firmware_installing));
			}
		});
	}
	
	@Override
	public void GetDeviceCode() {
	
	}
	
	@Override
	public void FirmwareInstalled() {
		Log.i(TAG, "FirmwareInstalled");
		handler.post(new Runnable() {
			@Override
			public void run() {
				bar.setIndeterminate(true);
				textViewMessage.setText(R.string.text_gamepad_is_rebooting);
			}
		});
	}
	
	@Override
	public void FirmwareFailed() {
		Log.i(TAG, "Firmware Failed");
		handler.post(new Runnable() {
			@Override
			public void run() {
				bar.setIndeterminate(false);
				bar.setProgress(0);
				textViewMessage.setText(R.string.text_firmware_need_power);
				Button button = findViewById(R.id.button_restart_action);
				button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						InstallFirmware();
						findViewById(R.id.button_restart_action).setVisibility(View.GONE);
					}
				});
				button.setVisibility(View.VISIBLE);
			}
		});
	}
}
