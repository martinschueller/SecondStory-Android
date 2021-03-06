package com.theonlyanimal.secondstory;

//IMPORTS

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.os.AsyncTask;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import net.hockeyapp.android.CrashManager;

import com.theonlyanimal.secondstory.StorageHelper;


// ACTIVITY CLASS

public class WelcomeScreen extends Activity {

	// GLOBALS
	private static final String TAG = "SS_WELCOME";
	private static final String APP_ID = "7f9de1a2655e56f6c60c798cc7d2cdec";
	private static final String SSID = "PHStheatre";
	private static final String PWD = "2ndst0ry";
	private static final String SD_DIRECTORY = "//sdcard//SecondStory/BloodAlley";
	private static final String MEDIA_DIRECTORY = "//sdcard//SecondStory/BloodAlley/MEDIA/";
	private static final String LOG_DIRECTORY = "//sdcard//SecondStory/BloodAlley/LOGS/";
	private static final String REMOTE_MEDIA_DIRECTORY = "/public_html/jessescott/projects/second_story/blood_alley/media";
	private static final String REMOTE_SETTINGS_DIRECTORY = "/public_html/jessescott/projects/second_story/blood_alley/settings";
    public static final int DIALOG_DOWNLOAD_PROGRESS = 0;
    private ProgressDialog progressDialog;
    private Button beginBtn;
    private Boolean hasEnoughSpace = false;
    private double 	needsThisMuchSpace = 0.5;
	
	// LifeCycle
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// UI
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome_layout);
		// Get Settings
		//RetrieveNecessarySize getSize = new RetrieveNecessarySize(); 
		//getSize.execute();		
		
		// Connect To WiFi
		//askForWifi();
		
		// Do We Have Content ?
		checkStorage();

		
	} /* onCreate() */
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Check for Crashes
		checkForCrashes();
	}
	
	// Hockey App
	private void checkForCrashes() {
		CrashManager.register(this, APP_ID); 
	}

	
	// WiFi
	public void connectToWifi() {
		
		// Configuration
		WifiConfiguration config = new WifiConfiguration();
		config.SSID = "\"" + SSID + "\"";
		config.preSharedKey = "\"" + PWD + "\""; // WPA2
		
		// Manager
		WifiManager manager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
		
		if(manager.isWifiEnabled() == false) {
			manager.setWifiEnabled(true);
			Toast.makeText(this, "Turning On WiFi", Toast.LENGTH_SHORT).show();
		}

		int networkID = -1;
		networkID = manager.addNetwork(config);		
		if(networkID < 0) {
			Log.v(TAG, "couldnt connect");
			Toast.makeText(this, "Couldn't Connec To SecondStory Network", Toast.LENGTH_SHORT).show();
		}
		else {
			Log.v(TAG, "added network");
			Toast.makeText(this, "Added SecondStory Network To Saved Networks", Toast.LENGTH_SHORT).show();
		}
		
		manager.enableNetwork(networkID, true);
		manager.saveConfiguration();
		manager.reconnect();
		
		Toast.makeText(this, "Connecting To SecondStory Network", Toast.LENGTH_SHORT).show();
		
		/*
		 	SupplicantState supState; 
			wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			supState = wifiInfo.getSupplicantState();
		 */
		
		/*
		new Timer().schedule(new TimerTask() {          
		    @Override
		    public void run() {
		        getFiles();      
		    }
		}, 2000);
		*/
		
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
		  @Override
		  public void run() {
			  getFiles(); 
		  }
		}, 500);
		
	}
	
	public void askForWifi() {
		// Alert
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		// Title
		builder.setTitle("About To Download Content");
		builder.setMessage("This app requires 500mb of content - can we turn on your WiFi and connect to our network?");
		builder.setCancelable(false);
		// Buttons
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               // User clicked OK button
		        	   Log.v(TAG, " - User Said YES! - ");
		        	   Toast.makeText(getApplicationContext(), "Thanks!", Toast.LENGTH_SHORT).show();
		        	   connectToWifi();
		           }
		       });
		builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               // User cancelled the dialog 
		        	   Toast.makeText(getApplicationContext(), "Ok.... We Warned You.", Toast.LENGTH_SHORT).show();
		           }
		       });

		// Show
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	
	// Check If Directory Exists
	public void checkStorage() {
		
		// Variables
		StorageHelper storage = new StorageHelper();
		
		// Check For SD Card
		if(storage.isExternalStorageAvailableAndWriteable()) {
			Log.v(TAG, " - SD EXISTS - CHECKING SPACE - ");
			
			if(storage.getSpaceOnExternalStorage() > needsThisMuchSpace) {
				Log.v(TAG, " - Free Space = " + storage.getSpaceOnExternalStorage());
	
				// Check For Custom Directory
				try{
					final File base_directory = new File(SD_DIRECTORY);
					final File media_directory = new File(MEDIA_DIRECTORY);
					final File log_directory = new File(LOG_DIRECTORY);
					if(media_directory.exists()) {
						Log.v(TAG, " - MEDIA Path Exists - ");
						if(media_directory.isDirectory()) {
							Log.v(TAG, " - And Its A Directory - ");
							
							// How Many Files Are There ?
							File[] listOfFiles = media_directory.listFiles();
							Log.v(TAG, "Media Directory has " + listOfFiles.length + " Files");
	
							// TODO check actual time stamp of files
							// If There's None
							if(listOfFiles.length == 0) {
								// Get 'Em
								//getFiles();
								 showStreamOrDownloadDialog();
							}
							// All Good To Proceed
							else {
								Intent intent = new Intent(WelcomeScreen.this, MenuScreen.class);
								startActivity(intent);
								finish();
							}
							
						} 
						else {
							Log.v(TAG, " - But Its Not Directory - ");
				        	   // Make The Directories
				        	   base_directory.mkdirs();
				        	   media_directory.mkdirs();
				        	   log_directory.mkdirs();
				        	   showStreamOrDownloadDialog();
						}
					} 
					else {
						Log.v(TAG, " - Path Doesnt Exist - ");
						
						
						
					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
			else {
				Log.v(TAG, " - SD DOESNT HAVE ENOUGH SPACE - ALERTING USER - ");
				// Alert
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				
				// Title
				builder.setTitle("NOT ENOUGH SPACE");
				builder.setMessage("this app requires 1/2 gb for our content - please free up space and try again");
				
				// Buttons
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				               // User clicked OK button
				        	   Log.v(TAG, " - User Said OK! - "); 
				        	   Intent intent = new Intent(Intent.ACTION_MAIN);
				        	   intent.addCategory(Intent.CATEGORY_HOME);
				        	   intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				        	   startActivity(intent);
				        	   finish();
				           }
				       });;

				AlertDialog dialog = builder.create();
				dialog.show();
			}

		}
		else {
			Log.v(TAG, " - SD ISNT AVAILABLE - ALERTING USER - ");
			// Alert
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			
			// Title
			builder.setTitle("STORAGE ISNT ACCESSIBLE");
			builder.setMessage("this app requires access to your phone's storage - please make sure it is mounted then relaunch the app");
			
			// Buttons
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			               // User clicked OK button
			        	   Log.v(TAG, " - User Said OK! - "); 
			        	   Intent intent = new Intent(Intent.ACTION_MAIN);
			        	   intent.addCategory(Intent.CATEGORY_HOME);
			        	   intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			        	   startActivity(intent);
			        	   finish();
			           }
			       });;

			AlertDialog dialog = builder.create();
			dialog.show();
		}
			
	} /* checkStorage() */
	
	private void showStreamOrDownloadDialog(){
		// Alert
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		// Title
		builder.setTitle("Content Doesn't Exist");
		builder.setMessage("this app requires custom content. You can download it upfront or stream it.");
		builder.setCancelable(false);
		// Buttons
		builder.setPositiveButton("Download now", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               // User clicked OK button
		        	   Log.v(TAG, " - User Said YES! - ");
		        	   
		        	   // Get The Files
		        	   askForWifi();
		        	   //getFiles();
		        	   SharedPreferences prefs = getSharedPreferences(
		        			      "com.theonlyanimal.secondstory", Context.MODE_PRIVATE);
		        	   prefs.edit().putBoolean("com.theonlyanimal.secondstory.stream", false).apply();
		        	   
		           }
		       });
		builder.setNegativeButton("Stream", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               // User cancelled the dialog - WERE GOING HOME
		        	   Log.v(TAG, " - User Said Steam");
			        	   SharedPreferences prefs = getSharedPreferences("com.theonlyanimal.secondstory", Context.MODE_PRIVATE);
			        	   prefs.edit().putBoolean("com.theonlyanimal.secondstory.stream", true).apply();
						Intent intent = new Intent(WelcomeScreen.this, MenuScreen.class);
						startActivity(intent);
						finish();
		           }
		       });

		// Show
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	// Download Files
	public void getFiles() {
		Log.v(TAG, " - getFiles() - ");
		Toast.makeText(getApplicationContext(), "Dowloading Files", Toast.LENGTH_SHORT).show();
		//DownloadHelper downloadHelper = new DownloadHelper(); 
		//downloadHelper.execute();
		Intent i= new Intent(WelcomeScreen.this,FTPService.class);
		startService(i);
		// Start Tutorial Screens
		Intent intent = new Intent(WelcomeScreen.this, MenuScreen.class);
		startActivity(intent);
		finish();
		//stopService(i);
	}

	// Show Progress
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
			case DIALOG_DOWNLOAD_PROGRESS:
				progressDialog = new ProgressDialog(this);
				progressDialog.setMessage("Downloading files... \nPlease be patient.");
				progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				progressDialog.setCancelable(false);
				progressDialog.show();
				return progressDialog;
			default:
				return null;
		}
	}
	
	
	/*
	 * ASYNC FTP CLASSES
	 */
	
	class RetrieveNecessarySize extends AsyncTask<Void, Void, String> {

		Double size;
		
		protected void onPreExecute(String unused) {
			Log.v(TAG, "PRE");
		}

		@Override
	    protected String doInBackground(Void... params) {
	    	Log.v(TAG, "BACKGROUND");
			size = 0.0;
			URL url = null;
			try {
				Log.v(TAG, "TRY");
				url = new URL("http://jesses.co.tt/projects/second_story/blood_alley/settings/size_of_media.txt");
				BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				String str;
				Log.v(TAG, "Line reads " + in.readLine());
				while ((str = in.readLine()) != null) {
					Log.v(TAG, "File Reads " + str);
				}
			} 
			catch (MalformedURLException e) {
				e.printStackTrace();
			}
			catch(IOException e) {
				e.printStackTrace();
			}
			return null;
	    }

		@Override
	    protected void onPostExecute(String unused) {
	    	Log.v(TAG, "POST");
	    	needsThisMuchSpace = size;
	    }


	}
	
	class DownloadHelper extends AsyncTask<String, Integer, String> {
		
		private static final String TAG = "FTP";
		private final FTPClient ftp;
		private static final String username = "ghostlight"; 
		private static final String password = "gh0st_l1ght"; 
		private static final String hostname = "ftp.memelab.ca"; 
		private static final int port = 21; 

		
		// Constructor
		DownloadHelper() {
			ftp = new FTPClient();
		}
		
		private Boolean setupFTP() {
			
			// Connect To Server
			if(connectToFTP()) {
				Log.v(TAG, "Connected");
			}
			else {
				Log.v(TAG, "Couldnt Connect");
				return false;
			}
			
			// Login
			if(loginToFTP()) {
				Log.v(TAG, "Logged In");
			}
			else {
				Log.v(TAG, "Couldnt Login");
				return false;
			}
			
			// Navigate To Correct Path
			if(navigateToPath()) {
				Log.v(TAG, "Navigated");
			}
			else {
				Log.v(TAG, "Couldnt Navigate");
				return false;
			}
			
			// Set To Binary File Type 
			try {
				ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
				ftp.setBufferSize(1024);
			} 
			catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			
			// Passive Mode For Firewall
			ftp.enterLocalPassiveMode();
			
			// If No Errors... 
			return true;
		}
		
		private Boolean connectToFTP() {
			try {
				ftp.connect(hostname, port);
				return true;
			} 
			catch (SocketException e) {
				e.printStackTrace();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
		
		private Boolean loginToFTP() {
			try {
				ftp.login(username, password);
				return true;
			} 
			catch (SocketException e) {
				e.printStackTrace();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
		
		private Boolean navigateToPath() {
			try {
				// TODO edit path / fix FTP
				ftp.changeWorkingDirectory("/public_html/jessescott/storage/android");
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
		
		private void getFilesFromFTP() {
			int count = 0;
			OutputStream output = null;
			
			try {
		    	FTPFile[] files = ftp.listFiles();
				int totalFiles = files.length;
				
				// Set Log Directory & File
				File logFile = new File(LOG_DIRECTORY + "logfile.txt");
				Log.v("FTP", "LOG should exist at " + logFile.getAbsolutePath());
				if(!logFile.exists()) {
					try {
						logFile.createNewFile();
					}
					catch(Exception e) {
						e.printStackTrace();
					}
				}
				
				
		    	for (FTPFile f : files) {

					// Log Names
					Log.v("FTP", f.toFormattedString());
					
					
					// Set Path
					String remoteFile = f.getName();
					Log.v("FTP", "REMOTE file " + remoteFile);
					String localFile = MEDIA_DIRECTORY;
					localFile += remoteFile;
					Log.v("FTP", " is being put in LOCAL path " + localFile);
				    output = new BufferedOutputStream(new FileOutputStream(localFile));
				    
					// Set Time
				    Time now = new Time(Time.getCurrentTimezone());
				    now.setToNow();
					
					// Set Logger
				    BufferedWriter logger = new BufferedWriter(new FileWriter(logFile, true)); 
				    logger.append("Download for " + localFile + " started at " + now.format("%k:%M:%S"));
				    logger.newLine();
				    Long startTime = System.currentTimeMillis();
				    
				    // HTTP
				    System.setProperty("http.keepAlive", "false");

				    // Get Files
	                Boolean success = ftp.retrieveFile(remoteFile, output);
	                if(success) {
	                	Log.v("FTP", "SUCCESS");
	                	
	                	now.setToNow();
	                	Long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
	                	
					    logger.append("Download for " + localFile + " finished at " + now.format("%k:%M:%S"));
					    logger.newLine();
					    logger.append("for an elapsedTime of " + elapsedTime + " seconds");
					    logger.newLine();
					    logger.newLine();
	                }
	                
	                // Update Progress
	                count++;
	                publishProgress((int) ((count / (float) totalFiles) * 100));
	                
	                // Close Logger
	                logger.flush();
	                logger.close();
	                
	                // Close Buffer 
	                output.close();
				 }
		     } 
		     catch (IOException e) {
				e.printStackTrace();
		     }
		     finally {
				 // Logout + Disconnect
				try {
					if(ftp != null) {
						output.close();
					}
					ftp.logout();
					ftp.disconnect();
					Log.v("FTP", "DISCONNECT");
					
					// TODO add end of logger
					
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
		     }
		}
		
		@SuppressWarnings("deprecation")
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			showDialog(DIALOG_DOWNLOAD_PROGRESS);
		}


		@Override
		protected String doInBackground(String... params) {
			try {
				if(setupFTP()) {
					getFilesFromFTP();
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		
		protected void onProgressUpdate(Integer... progress) {
			 progressDialog.setProgress(progress[0]);
		}

		@SuppressWarnings("deprecation")
		protected void onPostExecute(String unused) {
			dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
			Log.v(TAG, "ALL DONE!");
			Intent intent = new Intent(WelcomeScreen.this, MenuScreen.class);
			startActivity(intent);
			finish();
			
		}	

	} /* EOC */

	
} /* EOC */