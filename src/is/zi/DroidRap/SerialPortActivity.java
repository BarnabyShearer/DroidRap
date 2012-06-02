/*
 * Copyright 2012 b@Zi.iS
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package is.zi.DroidRap;

import is.zi.DroidRap.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android_serialport_api.SerialPort;

public abstract class SerialPortActivity extends Activity {
	
	protected SerialPort mSerialPort;
	protected BluetoothSocket mBluetoothPort;
	protected OutputStream mOutputStream;
	private BufferedReader mInputBuffer; 
	private ReadThread mReadThread;
	protected List<String> mOutputBuffer = Collections.synchronizedList(new ArrayList<String>());	
	private SendThread mSendThread;
	
	private class ReadThread extends Thread {

		@Override
		public void run() {
			super.run();
			while(!isInterrupted()) {
				try {
					if (mInputBuffer == null) return;
					onDataReceived(mInputBuffer.readLine());
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}
	
	private class SendThread extends Thread {

		@Override
		public void run() {
			super.run();
			while(!isInterrupted()) {
				if(mOutputBuffer.size()>0) {
					try {
						for(byte b : mOutputBuffer.remove(0).getBytes()) {
							mOutputStream.write(b);
							Thread.sleep(10);
						}
						mOutputStream.write('\n');
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void DisplayError(int resourceId) {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle("Error");
		b.setMessage(resourceId);
		b.show();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			if (mSerialPort == null && mBluetoothPort == null) {
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(SerialPortActivity.this);
				String path = sp.getString("DEVICE", "");
				if(path.contains("/")) {
					int baudrate = Integer.decode(sp.getString("BAUDRATE", "-1"));
					
					if ( (path.length() == 0) || (baudrate == -1)) {
						throw new InvalidParameterException();
					}

					mSerialPort = new SerialPort(new File(path), baudrate, 0);
				} else {
					final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //magic UUID for serial connection
					BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
					if (adapter == null || !adapter.isEnabled()) {
						throw new InvalidParameterException();
					}
					BluetoothDevice device = adapter.getRemoteDevice(path);
					mBluetoothPort = device.createRfcommSocketToServiceRecord(SERIAL_UUID);
					mBluetoothPort.connect();
				}
			}
			if (mSerialPort != null) {
				mOutputStream = mSerialPort.getOutputStream();
				mInputBuffer = new BufferedReader(new InputStreamReader(mSerialPort.getInputStream()));
			} else {
				mOutputStream = mBluetoothPort.getOutputStream();
				mInputBuffer = new BufferedReader(new InputStreamReader(mBluetoothPort.getInputStream()));
			}
			mReadThread = new ReadThread();
			mReadThread.start();
			
			mOutputBuffer = new ArrayList<String>();
			mSendThread = new SendThread();
			mSendThread.start();

		} catch (SecurityException e) {
			DisplayError(R.string.error_security);
			//startActivity(new Intent(SerialPortActivity.this, SerialPortPreferences.class));
		} catch (IOException e) {
			
			DisplayError(R.string.error_unknown);
			//startActivity(new Intent(SerialPortActivity.this, SerialPortPreferences.class));
		} catch (InvalidParameterException e) {
			DisplayError(R.string.error_configuration);
			startActivity(new Intent(SerialPortActivity.this, SerialPortPreferences.class));
		} catch (IllegalArgumentException e) {
			DisplayError(R.string.error_configuration);
			startActivity(new Intent(SerialPortActivity.this, SerialPortPreferences.class));
		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 0, 0, R.string.settings)
        	.setIcon(android.R.drawable.ic_menu_preferences);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
            	startActivity(new Intent(SerialPortActivity.this, SerialPortPreferences.class));
                return true;
        }
        return false;
    }
    
	protected abstract void onDataReceived(final String buffer);

	@Override
	protected void onDestroy() {
		if (mReadThread != null)
			mReadThread.interrupt();
		if (mSerialPort != null)
			mSerialPort.close();
		if(mBluetoothPort != null)
			try {
				mBluetoothPort.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		mBluetoothPort = null;
		mSerialPort = null;
		super.onDestroy();
	}
	
	protected interface BluetoothDevicePicker {
	    public static final String EXTRA_NEED_AUTH =
	            "android.bluetooth.devicepicker.extra.NEED_AUTH";
	    public static final String EXTRA_FILTER_TYPE =
	            "android.bluetooth.devicepicker.extra.FILTER_TYPE";
	    public static final String EXTRA_LAUNCH_PACKAGE =
	            "android.bluetooth.devicepicker.extra.LAUNCH_PACKAGE";
	    public static final String EXTRA_LAUNCH_CLASS =
	            "android.bluetooth.devicepicker.extra.DEVICE_PICKER_LAUNCH_CLASS";

	    /**
	     * Broadcast when one BT device is selected from BT device picker screen.
	     * Selected BT device address is contained in extra string {@link BluetoothIntent}
	     */
	    public static final String ACTION_DEVICE_SELECTED =
	            "android.bluetooth.devicepicker.action.DEVICE_SELECTED";

	    /**
	     * Broadcast when someone want to select one BT device from devices list.
	     * This intent contains below extra data:
	     * - {@link #EXTRA_NEED_AUTH} (boolean): if need authentication
	     * - {@link #EXTRA_FILTER_TYPE} (int): what kinds of device should be
	     *                                     listed
	     * - {@link #EXTRA_LAUNCH_PACKAGE} (string): where(which package) this
	     *                                           intent come from
	     * - {@link #EXTRA_LAUNCH_CLASS} (string): where(which class) this intent
	     *                                         come from
	     */
	    public static final String ACTION_LAUNCH =
	            "android.bluetooth.devicepicker.action.LAUNCH";

	    /** Ask device picker to show all kinds of BT devices */
	    public static final int FILTER_TYPE_ALL = 0;
	    /** Ask device picker to show BT devices that support AUDIO profiles */
	    public static final int FILTER_TYPE_AUDIO = 1;
	    /** Ask device picker to show BT devices that support Object Transfer */
	    public static final int FILTER_TYPE_TRANSFER = 2;
	}

}
