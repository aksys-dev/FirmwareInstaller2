package com.aksys.firmwareinstaller2.Gamepad;

public interface GamepadEvent {
	void ConnectionFail();
	void ConnectionSuccess();
	void GetFirmware();
	void GetBattery();
	void SendingFirmware(int sendbytes);
	void GetDeviceCode();
	void FirmwareInstalled();
	void FirmwareFailed();
}
