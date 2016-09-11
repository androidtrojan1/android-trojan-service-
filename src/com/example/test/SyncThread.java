package com.example.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class SyncThread implements Runnable{
	 String path;
	 String file_to_upload;
	 List<String> remotelist = new ArrayList<String>(); // массив файлов на сервере
	 List<String> locallist = new ArrayList<String>(); // массив локальных файлов
	 long synctime=0;
	 boolean no_files;
	 WakeLock wakeLock;
	 Context context;
	 boolean plain=false;
	 boolean auto=true;
		
	SyncThread(String path,Context context,boolean is_plain,boolean auto){ 
		this.path = path;
		this.context = context;
		this.plain =is_plain;
		this.auto = auto;}
	
	@Override
	public void run() {
		try{
			PowerManager mgr = (PowerManager)context.getSystemService(Context.POWER_SERVICE); // лочим
			wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SyncWakeLock");
			wakeLock.acquire();} catch(Exception e){
				Log.d(MyService.LOG_TAG, "error setting sync wake lock!");
				}
		
	try{
			MyService.syncactive=true;
			no_files=false;
			remotelist=load_url(MyService.filelist_url); // загружаем страницу с листингом файлов
			getlocalfiles(path); // получаем список файлов (args = папка для записи логов)
			for (int i=0; i<locallist.size();i++){
				file_to_upload=locallist.get(i);  // текущий проверяемый на синхронизированность файл
				if(remotelist.contains(file_to_upload)){
					Log.d(MyService.LOG_TAG, file_to_upload+" is present");
					if(auto){
						try{new File(path+"/"+file_to_upload).delete();}
						catch (Exception e){Log.d(MyService.LOG_TAG, "error deleting "+file_to_upload);}
						}
					
				}
				else{
					//Log.d(MyService.LOG_TAG, "adding uploader task for "+file_to_upload);
					if(!plain)
					{uploadfileenc(180,path+"/"+file_to_upload);}
					else{uploadfile(path+"/"+file_to_upload);}
				}
			}
			HiddenWaiter.lastsynctime=System.currentTimeMillis();
			Log.d(MyService.LOG_TAG, "sync done!");
	}
	catch(Exception e){
		Log.d(MyService.LOG_TAG,"exiting syncthread! (probably interrupted)");
	}
	try{wakeLock.release();}catch(Exception ex1){Log.d(MyService.LOG_TAG, "sync DIDNT release lock!");}
	MyService.syncactive=false;
	}	

	
	public void getlocalfiles(String sync_path){	
	try{
	 path=sync_path;
	 File f = new File(path);        
	 File file[] = f.listFiles();
	 Log.d(MyService.LOG_TAG, file.length+" files");
	 for (int i=0; i < file.length; i++)
	 {
		 locallist.add(file[i].getName());
	 }
		}
		catch(Exception e){
			Log.d(MyService.LOG_TAG, "error reading or no local files!");
		}
	if(locallist.size()==0)
		no_files=true;
	}
	
	
	static Boolean uploadfileenc(int delay_seconds, String file) throws InterruptedException{
			
			final ExecutorService service;
		    final Future<Boolean>  result;
		    Boolean ls=false;
		    
		    //service = Executors.newFixedThreadPool(1);
		    service = Executors.newSingleThreadExecutor();
		    
		    result   = service.submit(new FilePosterEnc(file));
		    try {
		        //Ожидание 10 секунд
		        ls = result.get(delay_seconds,TimeUnit.SECONDS); // время успешного выполнения, 0 - в случае ошибки
		    } 
		    catch(final InterruptedException ie){
		    	result.cancel(true);
		    	service.shutdownNow();
		    	throw ie;
		    }
		    catch(final Exception e) {
		    	Log.d(MyService.LOG_TAG, "upload file time in syncthread exceeded\n"+e.toString());
		    	result.cancel(true);
		    }
		    service.shutdownNow();
		    return ls;
		
	}
	
	static void uploadfile(String file) throws InterruptedException{
		Runnable uploader = new HttpPoster(file);
		Thread t = new Thread(uploader);
		try{t.start();
		t.join();} catch (InterruptedException ie){
			t.interrupt();
			throw ie;
		}
	}


	static public List<String> load_url(String url_string){
		List<String> res = new ArrayList<String>();
		try{
		  URL url = new URL(url_string);
	  	  HttpURLConnection connection = (HttpURLConnection)url.openConnection();
	  	  connection.setRequestMethod("GET");
	  	  if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
	  		  Log.d(MyService.LOG_TAG, "load_url() error: "+connection.getResponseCode());
	  	  }
	  	  BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	String inputLine;

	while ((inputLine = in.readLine())!=null&&!(inputLine.equals("END"))){
	res.add(inputLine);}
	in.close();
	connection.disconnect();
	//Log.d(LOG_TAG, "total: "+remotelist.size());
	}
	catch(Exception e){
		Log.d(MyService.LOG_TAG, "error loading url in syncthread\n"+e.toString());
		return null;
	}
	return res;
	    }
 
	
}
