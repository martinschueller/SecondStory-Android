package com.theonlyanimal.secondstory;

// IMPORTS

import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.widget.TextView;


// CLASS
public class SplashScreen extends Activity {

	// GLOBALS
	//private static final String TAG = "SS_SPLASH";
    ProgressDialog progress;
    Typeface dinBlack, dinMedium;
    TextView splashLabel;
    Boolean manuallyPreppedDevice = false;
	
	// LifeCycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);
        
		// Fonts
		dinBlack = Typeface.createFromAsset(getAssets(), "fonts/din alternate black.ttf");
		dinMedium = Typeface.createFromAsset(getAssets(), "fonts/din alternate medium.ttf"); 
		
		// Label
		splashLabel = (TextView) findViewById(R.id.splash_label);
		splashLabel.setTypeface(dinMedium);
        
		// Progress Dialog
        progress = ProgressDialog.show(this, "Checking Settings", "this will just take a second", true);
        
        // Timer
		Thread timer = new Thread(){
			@Override
			public void run() {
				//super.run();
				try { 
					sleep(2500);
			       
					// TODO progress here handler
			        
					if(manuallyPreppedDevice) {
						//Intent i = new Intent("android.intent.action.MENU");
			    		Intent i = new Intent(SplashScreen.this, MenuScreen.class);
						startActivity(i);
			    		finish();
					}
					else {
						checkSettings();
					}
			
				}
				catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		timer.start();
    
    }
    
    protected void checkSettings() {

    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean hasContent = settings.getBoolean("hasContent", false);   
        
        if(hasContent) {
        	progress.dismiss();
        	//Intent i = new Intent("android.intent.action.VIDEO");
        	Intent i = new Intent(SplashScreen.this, MenuScreen.class);
        	startActivity(i);
			finish();
        }
        else if(!hasContent) {
        	progress.dismiss();
        	Intent i = new Intent("android.intent.action.WELCOME");
    		startActivity(i);
    		finish();
        }
    }
    
    
} /* EOC */
