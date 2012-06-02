/*
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

import java.util.ArrayList;
import java.util.Arrays;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android_serialport_api.SerialPortFinder;

public class SerialPortPreferences extends PreferenceActivity {

	private SerialPortFinder mSerialPortFinder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.serial_port_preferences);
		
		mSerialPortFinder = new SerialPortFinder();

		// Devices
		final ListPreference devices = (ListPreference)findPreference("DEVICE");
        ArrayList<String> entries = new ArrayList<String>(Arrays.asList(mSerialPortFinder.getAllDevices()));
        ArrayList<String> entryValues = new ArrayList<String>(Arrays.asList(mSerialPortFinder.getAllDevicesPath()));
        
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
        		for(BluetoothDevice device : adapter.getBondedDevices()) {
        			entries.add(device.getName());
        			entryValues.add(device.getAddress());
        		}
        }
			
		devices.setEntries((String[])entries.toArray(new String[0]));
		devices.setEntryValues((String[])entryValues.toArray(new String[0]));
		devices.setSummary(devices.getValue());
		devices.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				preference.setSummary((String)newValue);
				return true;
			}
		});

		// Baud rates
		final ListPreference baudrates = (ListPreference)findPreference("BAUDRATE");
		baudrates.setSummary(baudrates.getValue());
		baudrates.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				preference.setSummary((String)newValue);
				return true;
			}
		});

	}
}
