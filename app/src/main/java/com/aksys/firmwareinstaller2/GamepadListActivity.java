package com.aksys.firmwareinstaller2;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aksys.firmwareinstaller2.Gamepad.AdapterGamepadList;
import com.aksys.firmwareinstaller2.Gamepad.GamepadInfo;
import com.aksys.firmwareinstaller2.Gamepad.GamepadList;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static com.aksys.firmwareinstaller2.Gamepad.GamepadList.SET_FW_ID;

public class GamepadListActivity extends AppCompatActivity {
	final String TAG = "GamepadUtility";
	RecyclerView recyclerView;
	RecyclerView.LayoutManager layoutManager;
	GamepadList gamepadList = GamepadList.getInstance();
	
	View clickedView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_gamepad_list );
		Log.i( TAG, "onCreate: GamepadListActivity" );
//
//		Toolbar toolbar = findViewById( R.id.toolbar );
//		setSupportActionBar( toolbar );
//		getSupportActionBar().setDisplayHomeAsUpEnabled( true );
		
		CheckResourceList();
		
		recyclerView = findViewById( R.id.gamepad_listview );
		recyclerView.setHasFixedSize( true );
		Log.i(TAG, "onCreate: widthDp: " + getResources().getConfiguration().screenWidthDp);
		layoutManager = new GridLayoutManager(this, 1 + (getResources().getConfiguration().screenWidthDp / 400) );
//		if (getResources().getConfiguration().screenWidthDp / 490 > 1) {
//
//		} else {
//			layoutManager = new LinearLayoutManager( this );
//		}
		recyclerView.setLayoutManager( layoutManager );
	}
	
	List<Integer> resourcelist;
	List<String> resourceNameList;
	
	void CheckResourceList() {
		resourcelist = new ArrayList<>();
		resourceNameList = new ArrayList<>();
		
		Field[] fields=R.raw.class.getFields();
		for(int count=0; count < fields.length; count++){
			Log.i("Raw Asset: ", fields[count].getName());
			try {
				resourcelist.add(fields[count].getInt(fields[count]));
				resourceNameList.add(fields[count].getName());
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
	
	String[] getResourcesStringArray() {
		String[] list = new String[resourceNameList.size()];
		for (int x = 0; x < resourceNameList.size(); x++) {
			list[x] = resourceNameList.get(x);
		}
		return resourceNameList.toArray(list);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		gamepadList.checkList();
		ShowGamepadLists(false);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		gamepadList.AllBluetoothConnectionClose();
	}
	
	void ShowGamepadLists(boolean allgamepads) {
		// ALREADY in GamepadService
		AdapterGamepadList adapterGamepadList;
		/// TODO: Get ArrayList from Service
		adapterGamepadList = new AdapterGamepadList( gamepadList.getList(), allgamepads);
		recyclerView.setAdapter( adapterGamepadList );
		
		if (gamepadList.getCounts(allgamepads) == 0) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.title_no_gamepad);
			builder.setIcon(R.drawable.ic_gamepaddisconnect);
			builder.setMessage(R.string.text_no_gamepad_description);
			builder.setPositiveButton(R.string.text_connect_bluetooth, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					dialogInterface.dismiss();
					startActivity( new Intent( Settings.ACTION_BLUETOOTH_SETTINGS ) );
				}
			});
			builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					dialogInterface.dismiss();
					finish();
				}
			});
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
		}
	}
	
	public void onClick(View view) {
		clickedView = view;
		if (view.getTag() != null) {
			final int x = (int)view.getTag();
			Log.i( TAG, "onClick: x = " + x );
			if (isTargetDevice(x)) {
				if (SET_FW_ID == -1) {
					ShowTargetFirmware(x);
				} else {
					GotoInstallFirmware(x, resourcelist.get(SET_FW_ID));
				}
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Error");
				builder.setIcon(R.drawable.ic_close_24dp);
				builder.setMessage(String.format("Not found Brand Name. Please re-check device name.\n- Spaces(^): %s\n- Brand Name: %s",
						gamepadList.getIndex(x).getGamepadName().replace(" ", "^"),
						checkTargetBrand(gamepadList.getIndex(x).getGamepadName())
				));
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builder.setNeutralButton("Continue Install", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ShowTargetFirmware(x);
					}
				});
				AlertDialog alertDialog = builder.create();
				alertDialog.show();
			}
		}
		view.setEnabled(false);
		view.getHandler().postDelayed(new Runnable() {
			@Override
			public void run() {
				clickedView.setEnabled(true);
			}
		}, 500);
	}
	
	int targetDevice = -1;
	void ShowTargetFirmware(int x) {
		targetDevice = x;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(gamepadList.getIndex(x).getGamepadName() + "\nSelect Firmware");
		builder.setIcon(R.drawable.ic_download);
		builder.setItems(getResourcesStringArray(), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				SET_FW_ID = i;
				GotoInstallFirmware(targetDevice, resourcelist.get(i));
			}
		});
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}
	
	private String checkTargetBrand(String gamepadName) {
		String[] checklist = getResources().getStringArray(R.array.checklist);
		for (int x = 0; x < checklist.length; x++) {
			if (gamepadName.toLowerCase().contains(checklist[x]))
				return getResources().getStringArray(R.array.gamepad_list)[x];
		}
		return "Cannot found our brand name.\nif you changed name from app, please reset gamepad first.";
	}
	
	boolean isTargetDevice(int x) {
		for (GamepadInfo g : gamepadList.getList()) {
			for (String brand : getResources().getStringArray(R.array.gamepad_list)) {
				if (g.getGamepadName().contains(brand)) return true;
			}
			
		}
		return false;
	}
	
	public void GotoInstallFirmware(int index, int firmware_index) {
		Intent intent = new Intent(this, FirmwareUpdateActivity.class);
		intent.putExtra(getString(R.string.intent_gamepad_addr), gamepadList.getIndex(index).getAddress());
		intent.putExtra("fw_file", firmware_index);
		startActivity(intent);
	}
	
//
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu_gamepadlist) {
//		MenuInflater menuInflater = getMenuInflater();
//		menuInflater.inflate( R.menu_gamepadlist.menu_bluetooth, menu_gamepadlist );
//		return true;
//	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() ) {
			case R.id.menu_bluetooth_setting:
				startActivity( new Intent( Settings.ACTION_BLUETOOTH_SETTINGS ) );
				return true;
//			case R.id.menu_select_firmware:
//				//TODO: Select Firmware
//				return true;
//			case R.id.action_all_gamepads:
//				if (item.getTitle().toString().contains( getString(R.string.action_all_gamepads))) {
//					gamepadList.checkList();
//					ShowGamepadLists(true);
//					item.setTitle(R.string.action_target_gamepads);
//					getSupportActionBar().setTitle(R.string.title_activity_all_gamepad_list);
//				} else {
//					ShowGamepadLists(false);
//					item.setTitle(R.string.action_all_gamepads);
//					getSupportActionBar().setTitle(R.string.title_activity_gamepad_list);
//				}
//				return true;
			default:
				return super.onOptionsItemSelected( item );
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_gamepadlist, menu);
		return true;
	}
}
