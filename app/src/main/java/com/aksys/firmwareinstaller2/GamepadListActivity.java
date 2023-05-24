package com.aksys.firmwareinstaller2;

import static com.aksys.firmwareinstaller2.Gamepad.GamepadList.SET_FW_ID;

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

public class GamepadListActivity extends AppCompatActivity {
	final String TAG = "GamepadUtility";
	RecyclerView recyclerView;
	RecyclerView.LayoutManager layoutManager;
	GamepadList gamepadList = GamepadList.getInstance();
	
	View clickedView;
	int resources;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_gamepad_list );
		Log.i( TAG, "onCreate: GamepadListActivity" );
//
//		Toolbar toolbar = findViewById( R.id.toolbar );
//		setSupportActionBar( toolbar );
//		getSupportActionBar().setDisplayHomeAsUpEnabled( true );
		
		recyclerView = findViewById( R.id.gamepad_listview );
		recyclerView.setHasFixedSize( true );
		Log.i(TAG, "onCreate: widthDp: " + getResources().getConfiguration().screenWidthDp);
		layoutManager = new GridLayoutManager(this, 1 + (getResources().getConfiguration().screenWidthDp / 600) );
//		if (getResources().getConfiguration().screenWidthDp / 490 > 1) {
//
//		} else {
//			layoutManager = new LinearLayoutManager( this );
//		}
		recyclerView.setLayoutManager( layoutManager );
	}
	
	List<Integer> resourceList;
	List<String> resourceNameList;
	
	int checkResourceList() {
		resourceList = new ArrayList<>();
		resourceNameList = new ArrayList<>();
		
		Field[] fields=R.raw.class.getFields();
		for (Field field : fields) {
			String name = field.getName();
			Log.i("Raw Asset: ", name);
			try {
				resourceList.add(field.getInt(field));
				resourceNameList.add(field.getName());
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return resourceList.size();
	}
	
	int checkResourceList(String target) {
		resourceList = new ArrayList<>();
		resourceNameList = new ArrayList<>();
		
		resourceList.clear();
		resourceNameList.clear();
		
		Field[] fields=R.raw.class.getFields();
		for (Field field : fields) {
			String name = field.getName();
			Log.i("FWAssets", "Raw Asset: " + name + " // target: " + target + " = " + name.contains(target + "_"));
			if (name.contains(target + "_")) {
				try {
					resourceList.add(field.getInt(field));
					resourceNameList.add(field.getName());
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return resourceList.size();
	}
	
	String UseCustomFile = "Use Custom Firmware Files";
	
	String[] getResourcesStringArray(int size) {
		ArrayList<String> list = new ArrayList<>();
		for (int x = 0; x < size; x++) {
			list.add(resourceNameList.get(x));
		}
		list.add(UseCustomFile);
		String[] arr = new String[list.size()];
		arr = list.toArray(arr);
		return arr;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		gamepadList.checkList(this);
		ShowGamepadLists(false);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		gamepadList.AllBluetoothConnectionClose();
	}
	
	void ShowGamepadLists(boolean allGamepads) {
		// ALREADY in GamepadService
		AdapterGamepadList adapterGamepadList;
		/// TODO: Get ArrayList from Service
		adapterGamepadList = new AdapterGamepadList( gamepadList.getList(), allGamepads);
		recyclerView.setAdapter( adapterGamepadList );
		
		if (gamepadList.getCounts(allGamepads) == 0) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.title_no_gamepad);
			builder.setIcon(R.drawable.ic_gamepaddisconnect);
			builder.setMessage(R.string.text_no_gamepad_description);
			builder.setPositiveButton(R.string.text_connect_bluetooth, (dialogInterface, i) -> {
				dialogInterface.dismiss();
				startActivity( new Intent( Settings.ACTION_BLUETOOTH_SETTINGS ) );
			});
			builder.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
				dialogInterface.dismiss();
				finish();
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
			GamepadInfo g = findTargetDevice(x);
			if (g != null) {
				Log.i(TAG, "onCreate: resources = " + resources);
				ShowTargetFirmware(x);
//				if (SET_FW_ID == -1 || resources < SET_FW_ID) {
//
//				} else {
//					//// Auto Select before installed FW.
//					GotoInstallFirmware(x, resourceList.get(SET_FW_ID));
//				}
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Error");
				builder.setIcon(R.drawable.ic_close_24dp);
				builder.setMessage(String.format("Not found Brand Name. Please re-check device name.\n- Spaces(^): %s\n- Brand Name: %s",
						gamepadList.getIndex(x).getGamepadName().replace(" ", "^"),
						checkTargetBrand(gamepadList.getIndex(x).getGamepadName())
				));
				builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
				builder.setNeutralButton("Continue Install", (dialog, which) -> ShowTargetFirmware(x));
				AlertDialog alertDialog = builder.create();
				alertDialog.show();
			}
		}
		view.setEnabled(false);
		view.getHandler().postDelayed(() -> clickedView.setEnabled(true), 500);
	}
	
	int targetDevice = -1;
	void ShowTargetFirmware(int x) {
		targetDevice = x;
		GamepadInfo g = gamepadList.getIndex(targetDevice);
		int sizes = checkResourceList(checkTargetFWName(g.getGamepadName()));
		if (resources == sizes && SET_FW_ID > -1) {
			GotoInstallFirmware(targetDevice, resourceList.get(SET_FW_ID));
		} else if (sizes > 0) {
			resources = sizes;
			if (!BuildConfig.DEBUG && resources == 1) {
				GotoInstallFirmware(targetDevice, resourceList.get(0));
			} else {
				Log.i(TAG, "ShowTargetFirmware: size = " + sizes);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(g.getGamepadName() + "\nSelect Firmware");
				builder.setIcon(R.drawable.ic_download);
				builder.setItems(getResourcesStringArray(sizes), (dialogInterface, i) -> {
					if (i < resourceList.size()) {
						SET_FW_ID = i;
						GotoInstallFirmware(targetDevice, resourceList.get(i));
					} else {
						GotoInstallFirmware(targetDevice);
					}
				});
				AlertDialog alertDialog = builder.create();
				alertDialog.show();
			}
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("NO FIRMWARE!");
			builder.setIcon(R.drawable.ic_baseline_insert_drive_file_24);
			if (BuildConfig.DEBUG) {
				builder.setMessage("Do you want install Custom Firmware?");
				builder.setPositiveButton(android.R.string.yes, (dialog, which) -> GotoInstallFirmware(targetDevice));
				builder.setNegativeButton(android.R.string.no, (dialog, which) -> {
					dialog.dismiss();
					finish();
				});
			}
			else {
				builder.setMessage("Cannot provide firmware for this product.\nPlease connect another product.");
				builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					dialog.dismiss();
					finish();
				});
			}
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
		}
	}
	
	private String checkTargetBrand(String gamepadName) {
		String[] checklist = getResources().getStringArray(R.array.checklist);
		for (int x = 0; x < checklist.length; x++) {
			if (gamepadName.toLowerCase().contains(checklist[x]))
				return getResources().getStringArray(R.array.gamepad_list)[x];
		}
		return "Cannot found our brand name.\nif you changed name from app, please reset gamepad first.";
	}
	
	private String checkTargetFWName(String gamepadName) {
		String[] checklist = getResources().getStringArray(R.array.gamepad_list);
		String[] searchList = getResources().getStringArray(R.array.checklist);
		for (int index = 0; index < checklist.length; index++) {
			if (gamepadName.contains(checklist[index])) return searchList[index];
		}
		return "Cannot found our brand name.\nif you changed name from app, please reset gamepad first.";
	}
	
	GamepadInfo findTargetDevice(int x) {
		for (GamepadInfo g : gamepadList.getList()) {
			for (String brand : getResources().getStringArray(R.array.gamepad_list)) {
				if (g.getGamepadName().contains(brand)) return g;
			}
		}
		return null;
	}
	
	public void GotoInstallFirmware(int index, int firmware_index) {
		Intent intent = new Intent(this, FirmwareUpdateActivity.class);
		intent.putExtra(getString(R.string.intent_gamepad_addr), gamepadList.getIndex(index).getAddress());
		intent.putExtra("fw_file", firmware_index);
		startActivity(intent);
	}
	
	public void GotoInstallFirmware(int index) {
		Intent intent = new Intent(this, FirmwareUpdateActivity.class);
		intent.putExtra(getString(R.string.intent_gamepad_addr), gamepadList.getIndex(index).getAddress());
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
		if (item.getItemId() == R.id.menu_bluetooth_setting) {
			startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_gamepadlist, menu);
		return true;
	}
}
