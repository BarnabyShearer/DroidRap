/*
 * Copyright 2012 b@Zi.iS
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package is.zi.DroidRap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class ManualActivity extends SerialPortActivity implements OnClickListener {

	@SuppressWarnings("serial")
	Map<Integer,String> cmds = new HashMap<Integer, String>() {{
		//put(R.id.bed_cold,"M140 S0");
		//put(R.id.bed_hot,"M140 S60");
		put(R.id.e, "G91\nG1 E5.0 F300\nG90");
		//put(R.id.e_cold, "M104 S0");
		//put(R.id.e_hot, "M104 S185");
		put(R.id.e_neg, "G91\nG1 E-5.0 F300\nG90");
		put(R.id.home, "G28\nG92 E0");
		put(R.id.status, "M105");
		put(R.id.stop, "M84");
		put(R.id.x, "G91\nG1 X10.0 F3000\nG90");
		put(R.id.x_neg, "G91\nG1 X-10.0 F3000\nG90");
		put(R.id.y, "G91\nG1 Y10.0 F3000\nG90");
		put(R.id.y_neg, "G91\nG1 Y-10.0 F3000\nG90");
		put(R.id.z, "G91\nG1 Z1.0 F200\nG90");
		put(R.id.z_neg, "G91\nG1 Z-1.0 F200\nG90");
	}};
	
	TextView mReception;
	TextView mBufLen;
	ProgressBar mETemp;
	ProgressBar mBedTemp;
	SeekBar mETempTarget;
	SeekBar mBedTempTarget;
	
	@Override
	public void onClick(View v) {
		if(v.getId()==R.id.stop) {
			mOutputBuffer.clear();
		}
		if(v.getId()==R.id.status) {
			mOutputBuffer.add(0, cmds.get(v.getId()));
		} else {
			mOutputBuffer.add(cmds.get(v.getId()));
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.manual);
		mBufLen = (TextView) findViewById(R.id.bufferlen);
		mReception = (TextView) findViewById(R.id.Reception);
		mReception.setMovementMethod(new ScrollingMovementMethod());
		mETemp = (ProgressBar) findViewById(R.id.e_temp);
		mBedTemp = (ProgressBar) findViewById(R.id.bed_temp);
		mETempTarget = (SeekBar) findViewById(R.id.e_temp_target);
		mBedTempTarget = (SeekBar) findViewById(R.id.bed_temp_target);
		mETempTarget.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				for(String buf: mOutputBuffer) {
					if (buf.startsWith("M104 S"))
						mOutputBuffer.remove(buf);
				}
				mOutputBuffer.add(0, "M104 S" + Integer.toString(progress) + "\n");
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		
		});
		mBedTempTarget.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				for(String buf: mOutputBuffer) {
					if (buf.startsWith("M140 S"))
						mOutputBuffer.remove(buf);
				}
				mOutputBuffer.add(0, "M140 S" + Integer.toString(progress) + "\n");
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		
		});
		for(int id: cmds.keySet()) {
			Button button = (Button)findViewById(id);
			button.setOnClickListener(this);
		}
		Button button = (Button)findViewById(R.id.print);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setType("file/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(Intent.createChooser(intent, getString(R.string.select_gcode)), 42);
			}
		});
		((EditText)findViewById(R.id.Emission)).setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId == EditorInfo.IME_ACTION_SEND) {
					mOutputBuffer.add(new String(v.getText().toString()));
				}
				return false;
			}
		});
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (resultCode == RESULT_OK) {
	        if (requestCode == 42) {
	        	InputStream instream;
				try {
					instream = new FileInputStream(new File(data.getData().getPath()));
					if (instream!=null) {
		        		InputStreamReader inputreader = new InputStreamReader(instream);
		        		BufferedReader buffreader = new BufferedReader(inputreader);
		        		String line;
		        		while ((line = buffreader.readLine())!=null) {
		        			mOutputBuffer.add(line);
		        		}
		        	}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
	        }
	    }
	}
	
	@Override
	protected void onDataReceived(final String buf) {
		runOnUiThread(new Runnable() {
			public void run() {
				if (mReception != null) {
					mBufLen.setText(Integer.toString(mOutputBuffer.size()));
					if(buf.startsWith("ok T:")) {
						mETemp.setProgress((int)Float.parseFloat(buf.split(":",3)[1].split("/",2)[0]));
						mETempTarget.setProgress((int)Float.parseFloat(buf.split(":",3)[1].split("/|\\s",3)[1]));
						mBedTemp.setProgress((int)Float.parseFloat(buf.split(":",3)[2].split("/",2)[0]));					                             
						mBedTempTarget.setProgress((int)Float.parseFloat(buf.split(":",3)[2].split("/|\\s",3)[1]));
					}
					mReception.append(buf + '\n');
					//Scroll to end
					final Layout layout = mReception.getLayout();
					if(layout != null){
						int scrollDelta = layout.getLineBottom(mReception.getLineCount() - 1) 
							- mReception.getScrollY() - mReception.getHeight();
						if(scrollDelta > 0)
							mReception.scrollBy(0, scrollDelta);
					}
				}
			}
		});
	}
}
