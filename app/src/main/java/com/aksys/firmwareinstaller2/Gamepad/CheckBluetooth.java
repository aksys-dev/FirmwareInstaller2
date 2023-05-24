package com.aksys.firmwareinstaller2.Gamepad;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;

import com.aksys.firmwareinstaller2.BuildConfig;

import java.util.Set;

@SuppressLint("MissingPermission")
public class CheckBluetooth {
	static BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
	private static final CheckBluetooth ourInstance = new CheckBluetooth();

	public static CheckBluetooth getInstance() {
		return ourInstance;
	}

	private CheckBluetooth() {
		adapter = BluetoothAdapter.getDefaultAdapter();
	}

	public static Intent CheckBluetoothDevice(String deviceName) {
		Intent intent = new Intent(  );
		Set<BluetoothDevice> devices = adapter.getBondedDevices();

		for (BluetoothDevice device : devices) {
			Log.d( "GamepadUtility_CKBT", device.getName() + " / " + device.getAddress());
			if ( ! device.getName().equals( deviceName ) ) continue;
			else if (device.getAddress().startsWith(BuildConfig.AKS_Bluetooth)) {
				intent.putExtra( "TYPE", GamepadInfo.TYPE_AKS_BT );
				intent.putExtra( "ADDR", device.getAddress() );
				intent.putExtra( "STAT", device.getBondState() );
				return intent;
			} else {
				intent.putExtra( "TYPE", GamepadInfo.TYPE_BLUETOOTH );
				intent.putExtra( "ADDR", device.getAddress() );
				intent.putExtra( "STAT", device.getBondState() );
				return intent;
			}
		}
		intent.putExtra( "TYPE", GamepadInfo.TYPE_USB );
		intent.putExtra( "ADDR", "-" );
		return intent;
	}

	public static BluetoothDevice getDevices(String deviceName) {
		if (adapter == null) adapter = BluetoothAdapter.getDefaultAdapter();
		Set<BluetoothDevice> devices = adapter.getBondedDevices();
		for (BluetoothDevice device : devices) {
			Log.d( "GamepadUtility_CKBT", device.getName() + " / " + device.getAddress());
			if ( ! device.getName().equals( deviceName ) ) continue;
			if (device.getAddress().startsWith( "00:1B:29" )) return device;
		}
		return null;
	}

	public static boolean HaveAKSGamepad() {
		Set<BluetoothDevice> devices = adapter.getBondedDevices();

		for (BluetoothDevice device : devices) {
			Log.d( "GamepadUtility_CKBT", device.getName() + " / " + device.getAddress());
			if (device.getAddress().startsWith( "00:1B:29" ) && device.getBondState() == BluetoothDevice.BOND_BONDED)
				return true;
		}
		return false;
	}

}
