package com.aksys.firmwareinstaller2.Gamepad;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;

import com.aksys.firmwareinstaller2.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

public class GamepadInfo {
	private final String TAG;
	public static final int TYPE_UNKNOWN = 0;
	public static final int TYPE_USB = 1;
	public static final int TYPE_BLUETOOTH = 2;
	public static final int TYPE_AKS_BT = 3;
	final String[] StringType = { "Unknown", "USB", "Bluetooth", "Bluetooth"};
	final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
	
	public static final int VIBRATION_MAX = 5;
	
	byte gamepadId;
	String gamepadName;
	int gamepadType;
	int bondStatus;
	float battery;
	String firmware;
	String Address;
	
	byte[] deviceType = new byte[] { 0x00, 0x00 };
	
	boolean ServiceHold = false;
	boolean ServiceConnect = false;
	
	int status;
	public static final int STATUS_NOT_CONNECT = -1;
	public static final int STATUS_IDLE = 0;
	public static final int STATUS_WRITING = 1;
	public static final int STATUS_COMPLETE = 2;
	
	public static final float AKS_BATTARY_MIN = 3000;
	public static final float AKS_BATTARY_MAX = 4100;
	
	BluetoothDevice device;
	BluetoothSocket socket;
	
	ConnectThread connectThread;
	IOSerialThread serialThread;
	
	Handler handler;
	GamepadEvent gamepadEvent;
	
	int ObuttonCode = 0;
	byte ObuttonTurbo = 0x00;
	
	GamepadInfo(int controllerID, String name) {
		gamepadName = name;
		gamepadId = (byte)controllerID;
		gamepadType = TYPE_UNKNOWN;
		ServiceHold = false;
		TAG = String.format( "Gamepad_%s", name );
		ServiceConnect = false;
		
		firmware = null;
		battery = 0;
		deviceType = new byte[] { -1, -1};
		handler = new Handler();
		gamepadEvent = null;
	}
	
	public GamepadInfo(int controllerID, String name, boolean hold) {
		gamepadName = name;
		gamepadId = (byte)controllerID;
		gamepadType = TYPE_UNKNOWN;
		ServiceHold = hold;
		TAG = String.format( "Gamepad_%s", name );
		ServiceConnect = false;
		
		firmware = null;
		battery = 0;
		deviceType = new byte[] { -1, -1};
		handler = new Handler();
		gamepadEvent = null;
	}
	
	public byte getGamepadId() {
		return gamepadId;
	}
	public String getGamepadName() {
		return gamepadName;
	}
	public int getGamepadType() {
		return gamepadType;
	}
	public int getGamepadDrawable() {
		switch (gamepadType) {
			case TYPE_AKS_BT:
				return R.drawable.ic_shaks_gamepad_logo;
			case TYPE_BLUETOOTH:
				return R.drawable.ic_gamepad_bluetooth;
			case TYPE_USB:
			default:
				return R.drawable.ic_gamepad_cable;
		}
	}
	public int getBondStatus() {
		return bondStatus;
	}
	public String getGamepadAddress() {
		return Address;
	}
	public String getGamepadTypeString() {
		return StringType[gamepadType];
	}
	public float getBattery() {
		return battery;
	}
	public String getFirmware() {
		return firmware;
	}
	public int getFirmwareInt() {
		if (firmware != null) return Integer.parseInt(firmware);
		else return -1;
	}
	public String getAddress() {
		return Address;
	}
	public int getStatus() {
		return status;
	}
	public byte getDeviceType() {
		return deviceType[1];
	}
	
	public boolean isServiceConnect() {
		return ServiceConnect;
	}
	public boolean isFirmwareDetected() {
		return firmware != null;
	}
	public boolean isBatteryLoaded() {
		return battery != 0;
	}
	public boolean isDeviceCodeLoaded() {
		if (isFirmwareDetected() && getFirmwareInt() < 1900000) return true;
		else if (isFirmwareDetected()) return deviceType[0] != -1;
		return false;
	}
	public boolean hasGamepadEvent() {
		return gamepadEvent != null;
	}
	
	public void setConnectType(int type) {
		if (type < 0 || type > StringType.length) type = 0;
		gamepadType = type;
	}
	
	public void setGamepadId() {
		gamepadId = STATUS_NOT_CONNECT;
		for (int deviceID : InputDevice.getDeviceIds()) {
			InputDevice d = InputDevice.getDevice(deviceID);
			if (d.getName().equals(gamepadName)) {
				gamepadId = (byte) d.getControllerNumber();
				break;
			}
		}
		Log.i(TAG, "setGamepadId: " + gamepadId);
	}
	public void setBondStatus(int bond) {
		bondStatus = bond;
	}
	
	public void setGamepadEvent(final GamepadEvent event) {
		gamepadEvent = event;
	}
	
	public void connectAKSGamepad(BluetoothDevice bluetoothDevice, boolean openSerial) {
		device = bluetoothDevice;
		if (device != null) setAddress( device.getAddress() );
//		if (device != null && openSerial && gamepadName.contains(BuildConfig.SHAKS_BRAND)) onConnectGamepad();
	}
	
	void setBattery(final float bat) {
		battery = bat;
		SetBattery();
		//Log.i( TAG, "setBattery: " + bat );
	}
	void setFirmware(final String fw) {
		firmware = fw;
		SetFirmware();
		//Log.i( TAG, "setFirmware: " + fw );
	}
	public void setAddress(final String address) {
		if (address != null) {
			Address = address;
			//Log.i( TAG, "setAddress: " + address );
		}
		else {
			Address = "Not Connected.";
		}
	}
	
	private byte[] savedbyte;
	
	public void onServiceHold(boolean hold) { ServiceHold = hold; }
	public synchronized void onConnectGamepad() {
		if ( connectThread == null ) {
			connectThread = new ConnectThread();
			connectThread.start();
		}
		else if (serialThread != null) {
			serialThread.StartCommand();
		}
	}
	
	public void onDisconnectGamepad() {
		if (serialThread != null) {
			serialThread.interrupt();
			SystemClock.sleep(10);
			try {
				serialThread.join();
			} catch (InterruptedException e){
				Log.d(TAG,"Connected join failed : " + e.getMessage());
			}
			serialThread = null;
		}
		
		if (connectThread != null) {
			connectThread.cancel();
			connectThread.interrupt();
			SystemClock.sleep(10);
			try {
				connectThread.join();
			} catch (InterruptedException e){
				Log.d(TAG,"Connect join failed : " + e.getMessage());
			}
			connectThread = null;
		}
		ServiceConnect = false;
		Log.i( TAG, "onDisconnectGamepad Completed." );
	}
	
	public synchronized void UpdateComplete() {
		SendByte( IOSerialThread.CMD_UPGRADE_SUCCESS);
		SystemClock.sleep(10);
	}
	public synchronized void CheckFirmware() {
		SendByte( IOSerialThread.CMD_QUERY_FW_VERSION);
	}
	public synchronized void CheckBattery() {
		SendByte( IOSerialThread.CMD_GET_BAT_V);
	}
	public synchronized void CheckDeviceCode() {
		SendByte( IOSerialThread.CMD_CHECK_GAMEPAD_INFO);
	}
	
	public static final int OBUTTON_NONE = 0;
	public static final int OBUTTON_CAMERA = 1;
	public static final int OBUTTON_SCREENSHOT = 2;
	public static final int OBUTTON_TURBO = 3;
	public synchronized void SetOButtonData() {
		Log.i(TAG, "SetOButtonData: ButtonCode: " + ObuttonCode + " / Turbo:" + Integer.toHexString(ObuttonTurbo));
		setButtonOption(ObuttonCode);
		setTurboOption(ObuttonTurbo);
	}
	
	public synchronized void SendByte(byte b) {
		if (serialThread != null) {
			serialThread.sendByte(b);
		} else {
			Log.i(TAG, "SendByte: Not Detect Serial Thread / Code: " + Integer.toHexString(b));
			ConnectionFail();
		}
	}
	
	public synchronized void SendByte(byte[] b) {
		if (serialThread != null) {
			serialThread.sendByteStream(b);
		} else {
			Log.i(TAG, "SendByte: Not Detect Serial Thread");
			ConnectionFail();
		}
	}
	
	public synchronized void onStartIMU() {
		SendByte( IOSerialThread.CMD_ENABLE_IMU );
	}
	public synchronized void onStopIMU() {
		SendByte( IOSerialThread.CMD_DISABLE_IMU );
	}
	public synchronized void setVibration(int left, int right) {
		if (left < 0 ) left = 0;
		if (left > VIBRATION_MAX ) left = VIBRATION_MAX;
		if (right < 0 ) right = 0;
		if (right > VIBRATION_MAX ) right = VIBRATION_MAX;
		
		SendByte(new byte[] { IOSerialThread.CMD_VIBRATE_ON, (byte)left, (byte)right });
	}
	public synchronized void setButtonOption(int keycode) {
		Log.i(TAG, "setButtonOption: " + keycode);
		ObuttonCode = keycode;
		if (isServiceConnect() && ObuttonCode != OBUTTON_TURBO) SendByte(new byte[] { IOSerialThread.CMD_SET_BUTTON, (byte) keycode});
	}
	
	public synchronized void setTurboOption(byte values) {
		Log.i(TAG, String.format("setTurboOption: %02x", values));
		ObuttonTurbo = values;
		if (isServiceConnect() && ObuttonCode == OBUTTON_TURBO) SendByte(new byte[]{ IOSerialThread.CMD_SET_TURBO, values});
	}
	
	public int getButtonOption() {
		return ObuttonCode;
	}
	
	public String getTurboOptionString() {
		StringBuffer stringBuffer = new StringBuffer();
		if ((ObuttonTurbo & 0x01) == 0x01) stringBuffer.append("A, ");
		if ((ObuttonTurbo & 0x02) == 0x02) stringBuffer.append("B, ");
		if ((ObuttonTurbo & 0x04) == 0x04) stringBuffer.append("X, ");
		if ((ObuttonTurbo & 0x08) == 0x08) stringBuffer.append("Y, ");
		if ((ObuttonTurbo & 0x10) == 0x10) stringBuffer.append("LB, ");
		if ((ObuttonTurbo & 0x20) == 0x20) stringBuffer.append("LT, ");
		if ((ObuttonTurbo & 0x40) == 0x40) stringBuffer.append("RB, ");
		if ((ObuttonTurbo & 0x80) == 0x80) stringBuffer.append("RT, ");
		if (stringBuffer.length() == 0)
			return null;
		else {
			stringBuffer.delete(stringBuffer.length() - 2, stringBuffer.length());
			Log.i(TAG, "getTurboOption: " + Integer.toHexString(ObuttonTurbo) + " / " + stringBuffer.toString());
			return stringBuffer.toString();
		}
	}
	
	public synchronized void setTurboOption(boolean[] values) {
		byte[] turboOption = new byte[] { IOSerialThread.CMD_SET_TURBO, 0x00};
		if (values.length != 8) return;
		if (values[0]) turboOption[1] += 0x01;
		if (values[1]) turboOption[1] += 0x02;
		if (values[2]) turboOption[1] += 0x04;
		if (values[3]) turboOption[1] += 0x08;
		if (values[4]) turboOption[1] += 0x10;
		if (values[5]) turboOption[1] += 0x20;
		if (values[6]) turboOption[1] += 0x40;
		if (values[7]) turboOption[1] += 0x80;
		Log.i(TAG, String.format("setTurboOption: %02x", turboOption[1]));
		ObuttonTurbo = turboOption[1];
		SendByte(turboOption);
	}
	
	private void SetGamepadIndicator() {
		Log.i( TAG, "Header:" + IOSerialThread.CMD_SET_CHANNEL + "; Channel:" + getGamepadId() );
		byte[] cmd = new byte[ 2 ];
		cmd[ IOSerialThread.INDEX_CMD ] = IOSerialThread.CMD_SET_CHANNEL;
		cmd[ IOSerialThread.INDEX_STATUS ] = (byte) ( getGamepadId() - 1 );
		SendByte( cmd );
	}
	
	public synchronized void TryFirmwareWrite(byte[] fwbyte) {
		savedbyte = fwbyte.clone();
		SendByte( IOSerialThread.CMD_ENABLE_UPDATE_MODE);
	}
	
	private synchronized void WriteFirmware() {
		Log.i(TAG, "WriteFirmware: ");
		if (savedbyte != null) SendByte( savedbyte );
	}
	
	public void setCalibration() {
		/// Only for TIM. but may need.
	}
	
	public void removePair() {
		if (device != null)
			try {
				Method m = device.getClass().getMethod("removeBond", (Class[]) null);
				m.invoke(device, (Object[]) null);
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
	}
	
	class ConnectThread extends Thread {
		ConnectThread() {
			BluetoothSocket tmp = null;
			try {
				tmp = device.createInsecureRfcommSocketToServiceRecord ( UUID.fromString( SPP_UUID ) );
			} catch ( IOException e ) {
				e.printStackTrace();
				ConnectionFail();
			}
			
			socket = tmp;
		}
		
		@Override
		public void run() {
			if(socket == null){
				Log.d(TAG,"csocket is null, ConnectThread fail ");
				ConnectionFail();
				return;
			}
			setName( "ConnectThread" );
			try {
				socket.connect();
				
			} catch (IOException e) {
				Log.d(TAG,"Socket Connect failed : " + e.getMessage());
				try{
					if(socket != null)
						socket.close();
				}catch (IOException e2){
					Log.d(TAG,"create socket failed : " + e2.getMessage());
				}
				ConnectionFail();
				cancel();
				return;
			}
			serialThread = new IOSerialThread( socket );
			serialThread.start();
		}
		
		void cancel() {
			try {
				if (socket != null) socket.close();
			} catch ( IOException e ) {
				Log.d( TAG, "cancel: Failed socket create: " + e.getMessage() );
			}
		}
	}
	class IOSerialThread extends Thread {
		
		private static final int INDEX_CMD = 0;
		private static final int INDEX_STATUS = 1;
		private static final int INDEX_LENGTH = 2;
		private static final int INDEX_DATA_START = 3;
		
		static final byte CMD_ENABLE_IMU = (byte) 0x80;
		static final byte CMD_DISABLE_IMU = (byte) 0x81;
		static final byte CMD_QUERY_IMU = (byte) 0x82;
		static final byte CMD_SET_CHANNEL = (byte) 0x83;
		static final byte CMD_QUERY_FW_VERSION = (byte) 0x84;
		static final byte CMD_START_FW_UPGRADE = (byte) 0x85;
		static final byte CMD_ENABLE_UPDATE_MODE = (byte) 0x8d;
		static final byte CMD_DISABLE_UPDATE_MODE = (byte) 0x8e;
		static final byte CMD_UPGRADE_SUCCESS = (byte) 0x8f;
		static final byte CMD_UPGRADE_FAILED = (byte) 0x90;
		static final byte CMD_VIBRATE_ON = (byte) 0x91;
		static final byte CMD_VIBRATE_OFF = (byte) 0x92;
		static final byte CMD_GET_BAT_V = (byte) 0x93;
		static final byte DATA_IMU_HEADER_PREFIX = (byte) 0x00;
		static final byte DATA_IMU_HEADER = (byte) 0x55;
		static final byte CMD_PARTITION_VERIFY_FAIL = (byte) 0x94;
		static final byte CMD_PARTITION_VERIFY_SUCCESS = (byte) 0x95;
		static final byte CMD_OTA_WRITTEN_BYTES = (byte) 0x96;
		static final byte CMD_OTA_DATA_RECEVIED = (byte) 0x97;
		static final byte CMD_ERROR_HEADER = (byte) 0x98;
		
		static final byte CMD_SET_BUTTON = (byte) 0x9c;
		static final byte CMD_SET_TURBO = (byte) 0x9d;
		//    public static final byte CMD_OTA_INTENT_REBOOT = (byte) 0x98;
		
		public static final byte CMD_CHECK_GAMEPAD_INFO = (byte) 0xff;
		
		private final BluetoothSocket socket;
		private final InputStream inputStream;
		private final OutputStream outputStream;
		
		IOSerialThread(BluetoothSocket _socket) {
			socket = _socket;
			InputStream i = null;
			OutputStream o = null;
			
			try {
				i = socket.getInputStream();
				o = socket.getOutputStream();
			} catch ( IOException e ) {
				e.printStackTrace();
			}
			
			inputStream = i;
			outputStream = o;
		}
		
		synchronized void sendByte(byte code) {
			try {
				Log.i( TAG, String.format( "Call: Send Code %x ...", code ) );
				outputStream.write( code );
			} catch ( IOException e ) {
				Log.w( TAG, "Call: Can't send message\n" + e.toString() );
				onDisconnectGamepad();
				status = STATUS_NOT_CONNECT;
				ConnectionFail();
			}
		}
		
		synchronized boolean sendByteStream(byte[] output) {
			try {
				Log.i( TAG, String.format( "Call: Send Code %x ... (size: %d)", output[0], output.length ) );
				outputStream.write( output );
			} catch ( IOException e ) {
				Log.w( TAG, "sendByteStream: Not Work\n" + e.toString() );
				ConnectionFail();
				return false;
			}
			return true;
		}
		
		synchronized void StartCommand() {
			UpdateComplete();
			SetGamepadIndicator();
			CheckFirmware();
			CheckBattery();
			CheckDeviceCode();
		}
		
		@Override
		public void interrupt() {
			Log.i( TAG, "interrupt: DISABLED" );
			super.interrupt();
		}
		
		@Override
		public void run() {
			Log.d( TAG, "run: connectedThread" );
			if (status == STATUS_NOT_CONNECT) status = STATUS_IDLE;
			ServiceConnect = true;
			StartCommand();
			int bytes;
			byte[] recv = new byte[22];
			boolean needUpdate;
			if (gamepadEvent != null) gamepadEvent.ConnectionSuccess();
			//String addMessage;
			while ( !Thread.currentThread().isInterrupted() ) {
				try {
					if (inputStream != null && ( inputStream.available() > 0)) {
						bytes = inputStream.read(recv);
						needUpdate = false;
						//addMessage = "";
						if ( recv[INDEX_CMD] == CMD_ENABLE_UPDATE_MODE ) {
							Log.d( TAG, "CMD_ENABLE_UPDATE_MODE" );
							status = STATUS_WRITING;
							//addMessage += "Device Updating...";
							WriteFirmware();
						} else if ( recv[INDEX_CMD] == CMD_DISABLE_UPDATE_MODE ) {
							Log.d( TAG, "CMD_DISABLE_UPDATE_MODE" );
							status = STATUS_IDLE;
						} else if ( recv[INDEX_CMD] == CMD_UPGRADE_SUCCESS ) {
							Log.d( TAG, "CMD_UPGRADE_SUCCESS" );
							status = STATUS_IDLE;
							//isCompleted = true;
							needUpdate = true;
						} else if ( recv[INDEX_CMD] == CMD_UPGRADE_FAILED ) {
							Log.d( TAG, "CMD_UPGRADE_FAILED" );
							status = STATUS_IDLE;
							needUpdate = true;
						} else if ( recv[INDEX_CMD] == CMD_GET_BAT_V ) {
							int volt = ( getIntfromByte( recv[ INDEX_DATA_START ], recv[ INDEX_DATA_START + 1 ] ) );
							float value;
							if ( volt >= 5000 ) value = volt;
							else
								value = ( ( (float) volt - AKS_BATTARY_MIN ) / ( AKS_BATTARY_MAX - AKS_BATTARY_MIN ) ) * 100;
							
							if (volt != value && value > 100) value = 100;
							if (value < 0) value = 0;
							
							setBattery( value );
							needUpdate = true;
//						} else if ( recv[INDEX_CMD] == DATA_IMU_HEADER_PREFIX && recv[1] == DATA_IMU_HEADER && bytes == 22 ) {
//							// Using For IMU Sensor Only.
//							if (dimention3.getReadyCalibrate()) {
//								inputGyroscopeData( getIntfromByte( recv[ 12 ], recv[ 13 ] ), getIntfromByte( recv[ 14 ], recv[ 15 ] ), getIntfromByte( recv[ 16 ], recv[ 17 ] ) );
//							} else {
//								dimention3.setVectorRaw(
//										getAccelometerFromByte( getIntfromByte( recv[ 6 ], recv[ 7 ] ), getIntfromByte( recv[ 8 ], recv[ 9 ] ), getIntfromByte( recv[ 10 ], recv[ 11 ] ) ),
//										getGyroscopeFromByte( getIntfromByte( recv[ 12 ], recv[ 13 ] ), getIntfromByte( recv[ 14 ], recv[ 15 ] ), getIntfromByte( recv[ 16 ], recv[ 17 ] ) ) );
////									-----------Code:| HEAD| LENG| TYPE| ACCELEROMETER   | GYROSCOPE       | CKSM| TAIL        
////									----------------------------------------------------------------------------------        
////									00 bytes / Code: 00 55 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00        
////									                ║  ║              ╚═X═╝ ╚═Y═╝ ╚═Z═╝ ╚═X═╝ ╚═Y═╝ ╚═Z═╝                    
////									       INDEX_CMD  IMU HEADER      Speed + Gravity    Rotation Speed
//							}
						} else if ( recv[INDEX_CMD] == CMD_OTA_WRITTEN_BYTES ) {
							Log.d( TAG, "run: Firmware Writing..." );
							SendingFirmware(getIntfromByte(recv[2], recv[3]));
						} else if ( recv[INDEX_CMD] == CMD_QUERY_FW_VERSION ) {
							setFirmware(new String(recv, INDEX_DATA_START, 6));
							Log.d(TAG, "Firmware: " + firmware);
							needUpdate = true;
						} else if ( recv[INDEX_CMD] == CMD_ENABLE_IMU) {
							Log.i(TAG, "CMD_ENABLE_IMU");
							SetIMUSensor(true);
						}
						else if ( recv[INDEX_CMD] == CMD_DISABLE_IMU) {
							Log.i(TAG, "CMD_DISABLE_IMU");
							SetIMUSensor(false);
						} else if ( recv[INDEX_CMD] == CMD_ERROR_HEADER ) {
							Log.w( TAG, "Firmware: CMD_ERROR_HEADER" );
							status = STATUS_COMPLETE;
							onDisconnectGamepad();
						} else if ( recv[INDEX_CMD] == CMD_OTA_DATA_RECEVIED ) {
							Log.d( TAG, "Firmware: CMD_OTA_END_TAG" );
						} else if ( recv[INDEX_CMD] == CMD_PARTITION_VERIFY_SUCCESS ) {
							Log.d( TAG, "Firmware: CMD_PARTITION_VERIFY_SUCCESS" );
							status = STATUS_COMPLETE;
							//addMessage += "*** Install Completed!";
							FirmwareInstalled();
							onDisconnectGamepad();
						}
						else if ( recv[INDEX_CMD] == CMD_CHECK_GAMEPAD_INFO ) {
							Log.i(TAG, String.format( "DEVICE: %02x %02x" , recv[3], recv[4] ) );
							//evented = true;
							/** Gamepad Detecter Code.
							 *  5 Byte: 00 00 00 00 00 --> HEAD , SUCCESS/FAIL , LENGTH , BrandCode , OPTIONAL
							 *  BrandCode: 0x01 - TIMGamepad v2, 0x02 - SHAKS S2
							 *
							 *  ┌ Optional Bit Table ------------------------------------------┐
							 *  7       6       5       4       3       2       1       0
							 *  0x80    0x40    0x20    0x10    0x08    0x04    0x02    0x01
							 *  -       -       -       -       -       -       Audio   Sensor
							 *  example: 0000 0000 - Not have option.
							 *  example: 0000 0001 - IMU Sensor haved Model.
							 *  example: 0000 0010 - No IMU Sensor, Have Audio Jack.
							 *  Please Check.
							 */
							deviceType = new byte[] {recv[3], recv[4]};
							SetDeviceCode();
						}
						else {
							Log.d( TAG, String.format( "run: Data - bytes: %d / CODE: %x", bytes, recv[ 0 ] ) );
						}
//						final boolean finalNeedUpdate = needUpdate;
//						final int finalFirmware = firmware;
//						final String finalAddMessage = addMessage;
						if (!ServiceHold && battery  > 1 && firmware != null) {
							Log.i( TAG, "All Data is Checked. Auto Close Thread." );
							onDisconnectGamepad();
						}
					} else {
						if (inputStream == null) Log.d( TAG, "run: inputStream not available." );
					}
				} catch ( IOException e ) {
					interrupt();
					Log.d( TAG, "run: disconnected\n" + e.toString() );
					onDisconnectGamepad();
				}
				SystemClock.sleep( 1 );
			}
			try {
				inputStream.close();
				outputStream.close();
			} catch ( IOException e ) {
				Log.d( TAG, "run: Stream close " + e.toString() );
			}
		}
		
		int getIntfromByte(byte high, byte low) {
			int value = ((high & 0xFF) << 8) + (low & 0xFF);
			if (value >= 32768) value -= 65536;
			return value;
		}
	}
	
	void ConnectionFail() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (gamepadEvent != null) gamepadEvent.ConnectionFail();
			}
		});
	}
	
	void SetFirmware() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (gamepadEvent != null) gamepadEvent.GetFirmware();
			}
		});
	}
	void SetBattery() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (gamepadEvent != null) gamepadEvent.GetBattery();
			}
		});
	}
	void SetDeviceCode() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (gamepadEvent != null) gamepadEvent.GetDeviceCode();
			}
		});
	}
	void SendingFirmware(final int values) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (gamepadEvent != null) gamepadEvent.SendingFirmware(values);
			}
		});
	}
	void FirmwareInstalled() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (gamepadEvent != null) gamepadEvent.FirmwareInstalled();
			}
		});
	}
	void SetIMUSensor(final boolean service) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (gamepadEvent != null) gamepadEvent.SetIMUSensor(service);
			}
		});
	}
}
