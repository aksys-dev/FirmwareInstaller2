package com.aksys.firmwareinstaller2.Gamepad;

import android.net.Uri;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FirmwareFile {
	AppCompatActivity responseActivity;
	String TAG = "FWFile";
	int filename;
	int fileSize;
	byte[] fileByteArray;
	
	public FirmwareFile(AppCompatActivity activity) {
		filename = 0;
		fileSize = 0;
		fileByteArray = null;
		responseActivity = activity;
	}
	
	public boolean setFile(int raw) {
		InputStream file = responseActivity.getResources().openRawResource( raw );
		return setFileDirect( file );
	}
	
	public boolean setFile(Uri uri) {
		try {
			InputStream file = responseActivity.getContentResolver().openInputStream( uri );
			return setFileDirect( file );
		} catch ( FileNotFoundException e ) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean setFile(String path) {
		try {
			FileInputStream file = new FileInputStream( path );
			return setFileDirect( file );
		} catch ( FileNotFoundException e ) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean setFileDirect(InputStream inputStream) {
		try {
			int len = inputStream.available();
			fileByteArray = new byte[len];
			fileSize = inputStream.read(fileByteArray);
			String name = new String(fileByteArray, 2, 6);
			filename = Integer.parseInt( name );
			Log.d( TAG, "setFile: " + filename + " / size =" + len );
			inputStream.close();
			return true;
		} catch (Exception e) {
			Log.w( TAG, "getFile: Cannot read file " +e.getMessage()  );
			return false;
		}
	}
	
	public byte[] getFileByteArray() { return fileByteArray; }
	public int getFirmwareVersion() { return filename; }
	public int getFileSize() {
		return fileSize;
	}
}
