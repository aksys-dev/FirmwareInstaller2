package com.aksys.firmwareinstaller2.Gamepad;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.aksys.firmwareinstaller2.BuildConfig;
import com.aksys.firmwareinstaller2.R;

import java.util.ArrayList;

public class AdapterGamepadList extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	public static class GamepadViewHolder extends RecyclerView.ViewHolder {
		/// GamepadVH에 대한 각 UI 정보
		ImageView icons;
		TextView gamepadName, gamepadNumber, gamepadType, gamepadBattery, gamepadFirmware, gamepadAddress;
		ImageButton gamepadSetting, gamepadfwbutton;
		CardView layout;
		
		GamepadViewHolder(View view) {
			super(view);
			
			layout = view.findViewById( R.id.gamepad_layer );
			icons = view.findViewById(R.id.gamepad_icon);
			gamepadName = view.findViewById(R.id.pad_name);
			gamepadNumber = view.findViewById(R.id.pad_number);
			gamepadType = view.findViewById(R.id.pad_connect);
			gamepadBattery = view.findViewById(R.id.pad_battery);
			gamepadFirmware = view.findViewById(R.id.pad_firmware);
			gamepadAddress = view.findViewById(R.id.pad_address);
		}
		public void setVisibility(int visibility) {
			layout.setVisibility(visibility);
		}
		public void SetIcon(int drawable) {
			icons.setImageResource( drawable );
		}
	}
	
	/// Gamepad Info 클래스를 통한 ArrayList
	private ArrayList<GamepadInfo> gamepadInfoArrayList;
	private boolean allgamepads;
	public AdapterGamepadList(ArrayList<GamepadInfo> gamepadInfoArrayList, boolean allgamepads) {
		this.gamepadInfoArrayList = gamepadInfoArrayList;
		this.allgamepads = allgamepads;
	}
	
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		//// RecyclerView 에 들어갈 Layout 지정
		View v = LayoutInflater.from( parent.getContext() ). inflate( R.layout.recycler_gamepad_info, parent, false );
		return new GamepadViewHolder( v );
	}
	
	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
		/// 리스트 뷰에 들어갈 정보를 입력하는 공간
		final GamepadViewHolder viewHolder = (GamepadViewHolder) holder;
		viewHolder.gamepadName.setText( gamepadInfoArrayList.get( position ).getGamepadName() );
		viewHolder.gamepadNumber.setText(gamepadInfoArrayList.get(position).getAddress());
		viewHolder.gamepadNumber.setVisibility(View.VISIBLE);
		viewHolder.gamepadType.setText(String.format(viewHolder.gamepadType.getContext().getString(R.string.text_connectiontype), gamepadInfoArrayList.get( position ).getGamepadTypeString()));
//		viewHolder.gamepadSetting.setVisibility( View.GONE );
		viewHolder.layout.setTag( position );
		
		if (gamepadInfoArrayList.get( position ).getGamepadType() == GamepadInfo.TYPE_AKS_BT) {
//			viewHolder.gamepadFirmware.setText(String.format(viewHolder.gamepadFirmware.getContext().getString(R.string.text_firmware), gamepadInfoArrayList.get(position).getFirmware()));
//			viewHolder.gamepadAddress.setText(String.format(viewHolder.gamepadAddress.getContext().getString(R.string.text_address), gamepadInfoArrayList.get(position).getAddress()));
//			viewHolder.gamepadSetting.setVisibility( View.VISIBLE );
			if ( gamepadInfoArrayList.get( position ).getGamepadName().contains("SHAKS"))
				viewHolder.SetIcon( R.drawable.ic_shaks_gamepad_logo );
//				viewHolder.gamepadBattery.setText( R.string.gamepad_charging );
//			else if ( gamepadInfoArrayList.get( position ).getFirmware() != null && gamepadInfoArrayList.get( position ).getBattery() > 0) {
//				viewHolder.gamepadBattery.setText( String.format( viewHolder.layout.getContext().getString( R.string.text_battery_value ) , gamepadInfoArrayList.get( position ).getBattery() ) );
//			}
			else {
				viewHolder.SetIcon(R.drawable.ic_gamepad_aks);
			}
			
			if ( gamepadInfoArrayList.get( position ).getBondStatus() < BluetoothDevice.BOND_BONDED )
				viewHolder.SetIcon( R.drawable.ic_gamepad_aks_disconnect );
		}
		
		if (gamepadInfoArrayList.get( position ).getGamepadType() == GamepadInfo.TYPE_BLUETOOTH) {
			viewHolder.gamepadBattery.setVisibility( View.GONE );//.setText( "Battery: " + gamepadInfoArrayList.get( position ).getBattery() );
			viewHolder.gamepadFirmware.setVisibility( View.GONE );//.setText( "Firmware: " + gamepadInfoArrayList.get( position ).getFirmware() );
			viewHolder.gamepadAddress.setText(String.format(viewHolder.gamepadAddress.getContext().getString(R.string.text_address), gamepadInfoArrayList.get(position).getAddress()));
			
			if ( gamepadInfoArrayList.get( position ).getBondStatus() < BluetoothDevice.BOND_BONDED )
				viewHolder.SetIcon( R.drawable.ic_gamepad_bluetooth_disconnect );
			else
				viewHolder.SetIcon( R.drawable.ic_gamepad_bluetooth );
			
			if (!allgamepads) viewHolder.setVisibility(View.GONE);
		}
		if (gamepadInfoArrayList.get( position ).getGamepadType() == GamepadInfo.TYPE_USB) {
			viewHolder.gamepadBattery.setVisibility( View.GONE );
			viewHolder.gamepadFirmware.setVisibility( View.GONE );//.setText( "Firmware: " + gamepadInfoArrayList.get( position ).getFirmware() );
			viewHolder.gamepadAddress.setVisibility( View.GONE );
			viewHolder.SetIcon( R.drawable.ic_gamepad_cable );
			
			if (!allgamepads) viewHolder.setVisibility(View.GONE);
		}
	}
	
	public boolean isAKSYSGamepad(RecyclerView.ViewHolder viewHolder, String devicename) {
		for (String b : viewHolder.itemView.getContext().getResources().getStringArray(R.array.gamepad_list)) {
			if (devicename.contains(b)) return true;
		}
		return false;
	}
	
	@Override
	public int getItemCount() { return gamepadInfoArrayList.size(); }
}
