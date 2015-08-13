package com.amaker.BTWaveForm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


import android.app.Activity;
import android.os.Bundle;

import android.widget.TextView;
import android.widget.Button;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.SurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class RTMonitorActivity extends Activity{
	private String dataStr = new String();
	private boolean enRead = false;
	public TextView tvDeviceName;
	public static TextView tvDeviceStatus;
	private SurfaceView sfvECG;
	
	private BTReadThread mReadThread = new BTReadThread(50);
	private Handler msgHandler;
	private DrawECGWaveForm mECGWF;
	
	private String revTmpStr = new String();
	public List<Float> ECGDataList = new ArrayList<Float>();
	public boolean ECGDLIsAvailable = true;
	private float ECGData = 0;
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rtmonitor);
		// Init Resources
		tvDeviceName = (TextView)findViewById(R.id.tvDeviceName);
		tvDeviceStatus = (TextView)findViewById(R.id.tvDeviceStatus);
		sfvECG = (SurfaceView)findViewById(R.id.sfvECG);
		mECGWF = new DrawECGWaveForm(sfvECG);
		
		//List<Float> ECGCacheData = new ArrayList<Float>();
		//mECGWF.DrawtoView(ECGCacheData);
		tvDeviceStatus.setText("Standby");
		
		try{
			if(BTConnectActivity.mBTSocket.getInputStream()!=null)
			{
				
				enRead = true;
				Log.d("enRead","isread:"+enRead+" startreadthread");
				mReadThread.start();
			}
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		Looper lp = Looper.myLooper();
		msgHandler = new MsgHandler(lp);
		
		// Setting Timer to Draw and Save data
		Timer mDSTimer = new Timer();
		TimerTask mDSTask = new TimerTask(){
			public void run(){
				Message msg = Message.obtain();
				msg.what = 1;
				msgHandler.sendMessage(msg);
			}
		};
		
		// Set Timer
		mDSTimer.schedule(mDSTask,1000,100);
	}
	
	
	@Override
	public void onDestroy()  
    {
		Log.d("destroy", "destroy");
		//Close Socket
		try {
			BTConnectActivity.mBTSocket.close();
			enRead = false;
//			mReadThread.destroy();
		} catch (IOException e) {
			e.printStackTrace();
		}
        super.onDestroy();  
    }
	
//	@Override
//	public void onStop(){		
//		mReadThread.stop();
//		super.onStop();
//	}
//	
//	@Override
//	public void onResume(){
//		super.onResume();
//		mReadThread.resume();
//	}
	
	// MsgHandler class to Update UI
	class MsgHandler extends Handler{
		public MsgHandler(Looper lp){
			super(lp);
		}
		
		@Override
		public void handleMessage(Message msg){
			switch(msg.what){
			case 0:
				tvDeviceStatus.setText((String)msg.obj);
				if(BTConnectActivity.mBTSocket!=null)
					try{
						tvDeviceName.setText(BTConnectActivity.mBTSocket.getRemoteDevice().getName());
					}
					catch(Exception e){
						e.printStackTrace();
					}
					break;
			case 1:
				if (ECGDataList.size() > 1){
					Log.d("ecgdatasize", ">1");
					List<Float> ECGCacheData = new ArrayList<Float>();
					ECGCacheData.addAll(ECGDataList);
					ECGDLIsAvailable = false;
					ECGDataList.clear();
					ECGDLIsAvailable = true;
					// Draw To View
					mECGWF.DrawtoView(ECGCacheData);					
				}
				break;
			}

		}
	}

	// Bluetooth Reading data thread
	class BTReadThread extends Thread{
		private int wait = 50;// Time to wait
		public BTReadThread(int wait){
			this.wait = wait;
		}
    
		
		public void run(){
			Log.d("enRead","isread:"+enRead);
			int counter=0;
			while(enRead){
				try{
					if (BTConnectActivity.mBTSocket.getInputStream()!=null)
					{
						String sendData="0.160;0.180;0.170;0.180;0.180;0.170;0.170;0.130;0.130;0.100;0.060;0.040;0.010;-0.010;-0.020;-0.020;-0.020;-0.030;0.000;0.000;0.000;0.020;0.030;0.040;0.030;0.070;0.050;0.060;0.050;0.050;0.050;0.050;0.060;0.050;0.050;0.030;";
						
						final OutputStream mmOutStream;
						OutputStream tmpOut = null;
						tmpOut =BTConnectActivity.mBTSocket.getOutputStream();
						mmOutStream = tmpOut;
						mmOutStream.write(sendData.getBytes());
						counter++;
						Log.d("counter", "counter:"+counter);
						Log.d("readdata","start to read data");
						BTConnectActivity.mBTSocket.getOutputStream().write(48);						
						byte[] tmp = new byte[1024];
						//final InputStream mmInStream;
						//mmInStream=BTConnectActivity.mBTSocket.getInputStream();
						//int len=mmInStream.read(tmp);
						int len = BTConnectActivity.mBTSocket.getInputStream().read(tmp,0,1024);
						Log.d("length:", "len:"+len);
						if (len > 0){
							byte[] tmp2 = new byte[len];
							tmp2 = tmp;
							String str = new String(tmp2);
							revTmpStr = revTmpStr + str;
							if(revTmpStr.indexOf(';')!=-1){
								try{
									String ECGDataStrs[] = revTmpStr.split(";");
									//String ECGDataStrs={0.160;0.180;0.170;0.180;0.180;0.170;0.170;0.130;0.130;0.100;0.060;0.040;0.010;-0.010;-0.020;-0.020;-0.020;-0.030;0.000;0.000;0.000;0.020;0.030;0.040;0.030;0.070;0.050;0.060;0.050;0.050;0.050;0.050;0.060;0.050;0.050;0.030;};
									for (int i = 0; i < ECGDataStrs.length -1; i++){
										try{
											ECGData = Float.parseFloat(ECGDataStrs[i].replace(';',' '));
											Log.d("receivingdata","ECGDATA:"+ECGData);
											ECGDataList.add(ECGData);											
										}catch(Exception e){
											e.printStackTrace();
											continue;
										}

									}
									if (ECGDataStrs[ECGDataStrs.length -1].length()==6 || ECGDataStrs[ECGDataStrs.length -1].length()==7&&ECGDataStrs[ECGDataStrs.length -1].indexOf('-')==0){
										try{
											ECGData = Float.parseFloat(ECGDataStrs[ECGDataStrs.length -1].replace(';',' '));
											ECGDataList.add(ECGData);
										}catch(Exception e){
											e.printStackTrace();
										}
										revTmpStr = "";
									}
									else{
										revTmpStr = ECGDataStrs[ECGDataStrs.length -1];
									}									
									
								}
								catch(Exception e){
									e.printStackTrace();
								}								

							}
							
//							 Update UI
							Log.d("updateUI","updateUI");
							Message msg = Message.obtain();
							msg.what = 0;
							msg.obj = new String("Receiving Data");							
							msgHandler.sendMessage(msg);
						}
//						Log.d("updateUI","updateUI");
//						Message msg = Message.obtain();
//						msg.what = 0;
//						msg.obj = new String("Receiving Data");							
//						msgHandler.sendMessage(msg);
					}
					Thread.sleep(wait);
				}
				catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}
}
