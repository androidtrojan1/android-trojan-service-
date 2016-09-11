package com.example.test;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.Callable;

import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;

public class FilePosterEnc implements Callable<Boolean>{
	
	private String file_to_upload;
	private final static short offset = 33; // offset for encryption (choose your own)
	private final static byte[] PASSWORD = "YOUR_SUPER_LONG_COOL_PASSWORD_".getBytes();
	private final static int mod = PASSWORD.length;
		
	public FilePosterEnc(String file) {
       file_to_upload=file;
   }	
	
	public Boolean call() {
		
		long curtime=System.currentTimeMillis(); // текущее время = seed для шифровки
	  	  HttpURLConnection connection = null;
	  	  DataOutputStream outputStream = null;
	  	  String lineEnd = "\r\n";
	  	  String twoHyphens = "--";
	  	  String boundary =  "*****";
	  	   
	  	  int bufferSize = 1024;
	  	  int bytesRead;
	  	   
	  	  try
	  	  {
	  	      byte[] buffer = new byte[bufferSize];
	  	      byte[] key = new byte[bufferSize];
	  	      FileInputStream fileInputStream = new FileInputStream(new File(file_to_upload) );
	  	      URL url = new URL(MyService.uploader_url);
	  	      connection = (HttpURLConnection) url.openConnection();
	  	      connection.setDoInput(true);
	  	      connection.setDoOutput(true);
	  	      connection.setUseCaches(false);
	  	      connection.setChunkedStreamingMode(bufferSize);
	  	      connection.setRequestMethod("GET");
	  	      connection.setRequestProperty("Connection", "Keep-Alive");
	  	      connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
	  	      //application/octet-stream   multipart/form-data
	  	      connection.setRequestProperty("Time", Long.toString(curtime));  // передаем seed
	  	      outputStream = new DataOutputStream(connection.getOutputStream());
	  	      outputStream.writeBytes(twoHyphens + boundary + lineEnd);
	  	      outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\""+file_to_upload +"\"" + lineEnd);
	  	      outputStream.writeBytes(lineEnd);
	  	      Base64OutputStream b64 = new Base64OutputStream(outputStream,Base64.NO_PADDING|Base64.NO_WRAP);
	  	      bytesRead = fileInputStream.read(buffer, 0, bufferSize);
	  	   
	  	      while (bytesRead > 0)
	  	      {	  
	  	    	new Random(curtime+offset).nextBytes(key);  // заполняем массив ключа
	  			for (int i=0;i<bytesRead;i++){
	  			   buffer[i] = (byte)(buffer[i] ^ key[i] ^ PASSWORD[i % mod]);}
	  			 b64.write(buffer, 0, bytesRead);
	  	          bytesRead = fileInputStream.read(buffer, 0, bufferSize);
	  	          if(Thread.currentThread().isInterrupted()){
	  	        	fileInputStream.close();
	  	        	outputStream.close();
	  	        	  throw new InterruptedException();}
	  	      }
	  	      b64.flush();
	  	      outputStream.writeBytes(lineEnd);
	  	      outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
	  	      int serverResponseCode = connection.getResponseCode();
	  	      if (serverResponseCode==200){
	  	    	Log.d(MyService.LOG_TAG, "file "+file_to_upload+" uploaded successfully!"); 
	  	      }
	  	      else{
	  	    	Log.d(MyService.LOG_TAG, "file "+file_to_upload+" wasnt uploaded correctly");
	  	    	String serverResponseMessage = connection.getResponseMessage();
	    	      Log.d(MyService.LOG_TAG, "response code: "+serverResponseCode);
	    	      Log.d(MyService.LOG_TAG, "message:\n"+serverResponseMessage);
	  	      }
	  	      
	  	      fileInputStream.close();
	  	      outputStream.flush();
	  	      outputStream.close();
	  	  }
	  	  catch (Exception e)
	  	  {
	  		Log.d(MyService.LOG_TAG, "error uploading file "+file_to_upload+"\n"+e.toString());
	  		return false;
	  	  }
	  	  return true;
	   }		
	
}
