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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
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

	public static String PACKAGE_NAME;
	
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
		PACKAGE_NAME = getApplicationContext().getPackageName();
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
		button = (Button)findViewById(R.id.slice);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setType("file/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(Intent.createChooser(intent, getString(R.string.select_gcode)), 43);
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
	
	private void copyFileOrDir(String path) {
	    AssetManager assetManager = this.getAssets();
	    String assets[] = null;
	    try {
	        assets = assetManager.list(path);
	        if (assets.length == 0) {
	        	copyAssetFile(path);
	        } else {
	            String fullPath = "/data/data/" + PACKAGE_NAME + "/" + path;
	            File dir = new File(fullPath);
	            if (!dir.exists())
	                dir.mkdir();
	            for (int i = 0; i < assets.length; ++i) {
	                copyFileOrDir(path + "/" + assets[i]);
	            }
	        }
	    } catch (IOException ex) {
	        Log.e("tag", "I/O Exception", ex);
	    }
	}

	private void copyAssetFile(String filename) {
	    AssetManager assetManager = this.getAssets();

	    InputStream in = null;
	    OutputStream out = null;
	    try {
	        in = assetManager.open(filename);
	        String newFileName = "/data/data/" + PACKAGE_NAME + "/" + filename;
	        out = new FileOutputStream(newFileName);

	        byte[] buffer = new byte[1024];
	        int read;
	        while ((read = in.read(buffer)) != -1) {
	            out.write(buffer, 0, read);
	        }
	        in.close();
	        in = null;
	        out.flush();
	        out.close();
	        //new File(newFileName).setExecutable(true);
	        out = null;
	    } catch (Exception e) {
	        Log.e("tag", e.getMessage());
	    }

	}
	
	private void copyFile(String from, String to) {
	    InputStream in = null;
	    OutputStream out = null;
	    try {
	        in = new FileInputStream(from);
	        out = new FileOutputStream(to);

	        byte[] buffer = new byte[1024];
	        int read;
	        while ((read = in.read(buffer)) != -1) {
	            out.write(buffer, 0, read);
	        }
	        in.close();
	        in = null;
	        out.flush();
	        out.close();
	        out = null;
	    } catch (Exception e) {
	        Log.e("tag", e.getMessage());
	    }

	}
	
	private File getExternalFilesDir(String type) {
		String packageName = getApplicationContext().getPackageName();
		File externalPath = Environment.getExternalStorageDirectory();
		return new File(externalPath.getAbsolutePath() +
		                         "/Android/data/" + packageName + "/files");
	}
	private class SliceTask extends AsyncTask<String, String, Void> {
		
		@Override
		protected Void doInBackground(String... path) {
			try {
				ProcessBuilder builder = new ProcessBuilder();
				builder.redirectErrorStream(true);
	    		builder.command("/system/bin/su");
	    		
				if (!new File("/data/data/" + PACKAGE_NAME + "/min/slic3r").exists()) {
					publishProgress("Installing Slic3r...\n");
					copyFileOrDir("min");
				}
				if (!new File(getExternalFilesDir(null).getCanonicalPath() + "/Slic3r.cfg").exists()) {
					publishProgress("Configuring Slic3r...\n");
					Process su = builder.start();
					BufferedReader reader = new BufferedReader(new InputStreamReader(su.getInputStream()));
					String cmd = "chmod -R 755 /data/data/" + PACKAGE_NAME  + "/min\n";
					cmd += "chroot /data/data/" + PACKAGE_NAME  + "/min /slic3r --save /Slic3r.cfg\n";
					cmd += "exit\n";
					su.getOutputStream().write(cmd.getBytes());
					char[] buffer = new char[64];
					int read;
					while ((read = reader.read(buffer)) > 0) {
						publishProgress(new String(buffer,0, read));
					}
					reader.close();
					su.waitFor();
				} else {
					copyFile(getExternalFilesDir(null).getCanonicalPath() + "/Slic3r.cfg", "/data/data/" + PACKAGE_NAME + "/min/Slic3r.cfg");
				}
				if(path[0].endsWith(".stl")) {
					copyFile(path[0], "/data/data/" + PACKAGE_NAME + "/min/temp.stl");
				} else {
					String ext = path[0].substring(path[0].lastIndexOf(".")+1,path[0].length());
					copyFile(path[0], "/data/data/" + PACKAGE_NAME + "/min/temp." + ext);
					publishProgress("Converting from " + ext);
					Process su = builder.start();
					BufferedReader reader = new BufferedReader(new InputStreamReader(su.getInputStream()));
					String cmd = "chroot /data/data/" + PACKAGE_NAME  + "/min /ivcon /temp." + ext + " /temp.stl\n";
					cmd += "exit\n";
					su.getOutputStream().write(cmd.getBytes());
					char[] buffer = new char[64];
					int read;
					while ((read = reader.read(buffer)) > 0) {
						publishProgress(new String(buffer,0, read));
					}
					reader.close();
					su.waitFor();
				}
				publishProgress("Slicing...\n");
				Process su = builder.start();
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(su.getInputStream()));
				String cmd = "chroot /data/data/" + PACKAGE_NAME  + "/min /slic3r --load /Slic3r.cfg /temp.stl\nexit\n";
				su.getOutputStream().write(cmd.getBytes());
				char[] buffer = new char[64];
				int read;
				while ((read = reader.read(buffer)) > 0) {
					publishProgress(new String(buffer,0, read));
				}
				reader.close();
				su.waitFor();
				
				copyFile("/data/data/" + PACKAGE_NAME + "/min/Slic3r.cfg", getExternalFilesDir(null).getCanonicalPath() + "/Slic3r.cfg");
				copyFile("/data/data/" + PACKAGE_NAME + "/min/temp.gcode", path[0] + ".gcode");
				if(!path[0].endsWith(".stl")) {
					copyFile("/data/data/" + PACKAGE_NAME + "/min/temp.stl", path[0] + ".stl");
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(String... progress) {
			mReception.append(progress[0]);
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
	        } else if (requestCode == 43) {
	        	new SliceTask().execute(data.getData().getPath());
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
