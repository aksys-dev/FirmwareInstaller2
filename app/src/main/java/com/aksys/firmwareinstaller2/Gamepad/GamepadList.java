package com.aksys.firmwareinstaller2.Gamepad;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import com.aksys.firmwareinstaller2.BuildConfig;

import java.util.ArrayList;

import static com.aksys.firmwareinstaller2.Gamepad.GamepadInfo.TYPE_AKS_BT;

public class GamepadList {
	private static final GamepadList ourInstance = new GamepadList();

	public static GamepadList getInstance() {
		return ourInstance;
	}

	public static int SET_FW_ID = -1;

	ArrayList<GamepadInfo> GAMEPAD_LIST;

	private GamepadList() {
		GAMEPAD_LIST = new ArrayList<>();
	}

	public void checkList(Context context) {
		GAMEPAD_LIST.clear();
//		int[] deviceIDs = InputDevice.getDeviceIds();
//		for (int deviceID : deviceIDs) {
//			InputDevice device = InputDevice.getDevice( deviceID );
//			//if (findGamepadFromName( device.getName() ) != null) continue;
//			if ((device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
//				Log.i( "GamepadList", String.format( "checkList: %s", device.getName() ) );
//				GamepadInfo gamepadInfo = new GamepadInfo( device.getControllerNumber(), device.getName() );
//				Intent btcheck = CheckBluetooth.CheckBluetoothDevice( device.getName() );
//				int type = btcheck.getIntExtra( "TYPE", 0 );
//				if (type == TYPE_AKS_BT ) {
//					gamepadInfo.setConnectType( type );
//					gamepadInfo.setBondStatus( btcheck.getIntExtra( "STAT", BluetoothDevice.BOND_NONE ) );
//					gamepadInfo.setAddress( btcheck.getStringExtra( "ADDR" ) );
//					gamepadInfo.connectAKSGamepad( CheckBluetooth.getDevices( device.getName() ), false );
//					GAMEPAD_LIST.add( gamepadInfo );
//				}
//			}
//		}

		if (checkBluetoothPermission(context)) {
			for (BluetoothDevice device : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
				if (!device.getAddress().contains(BuildConfig.AKS_Bluetooth)) continue;

				GamepadInfo gamepadInfo = new GamepadInfo(-1, device.getName());
				gamepadInfo.setBondStatus(device.getBondState());
				gamepadInfo.setAddress(device.getAddress());
				gamepadInfo.connectAKSGamepad(CheckBluetooth.getDevices(device.getName()));
				gamepadInfo.setConnectType(TYPE_AKS_BT);
				GAMEPAD_LIST.add(gamepadInfo);

			}
		}
	}

	public static boolean checkBluetoothPermission(Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			return context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
		} else return true;
	}

	public ArrayList<GamepadInfo> getList() { return GAMEPAD_LIST; }
	
	public GamepadInfo getIndex(int index) {
		if (index < 0) return null;
		else return GAMEPAD_LIST.get( index );
	}
	
	public GamepadInfo findGamepadFromName(String devicename) {
		for ( GamepadInfo gamepad : GAMEPAD_LIST ) {
			if ( gamepad.getGamepadName().equals( devicename ) ) return gamepad;
		}
		return null;
	}
	
	public GamepadInfo findGamepadFromBluetoothAddr(String deviceaddr) {
		for ( GamepadInfo gamepad : GAMEPAD_LIST ) {
			if ( gamepad.getGamepadAddress().equals( deviceaddr ) ) return gamepad;
		}
		return null;
	}
	
	public void AllBluetoothConnectionClose() {
		for ( GamepadInfo gamepad : GAMEPAD_LIST ) {
			if ( gamepad.getGamepadType() == TYPE_AKS_BT ) {
				gamepad.onDisconnectGamepad();
			}
		}
	}
	
	public int getCounts(boolean all_gamepad) {
		if (all_gamepad) return GAMEPAD_LIST.size();
		else {
			int x = 0;
			for ( GamepadInfo gamepad : GAMEPAD_LIST ) {
				if ( CheckBluetooth.getDevices(gamepad.getGamepadName()) != null) x++;
			}
			return x;
		}
	}
}
