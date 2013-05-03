package de.mkrtchyan.recoverytools;

/*
 * Copyright (c) 2013 Ashot Mkrtchyan
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights 
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
 * copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.sbstrm.appirater.Appirater;

public class MainActivity extends Activity {
	
//	Get path to external storage
	private static final File PathToSd = Environment.getExternalStorageDirectory();
//	Declaring needed files and folders
	private static final File PathToRecoveryTools = new File(PathToSd , "RecoveryTools");
	private static final File PathToRecoveries = new File(PathToRecoveryTools ,"recoveries");
	private static final File PathToBackup = new File(PathToRecoveryTools, "backup");
	private static final File fBACKUP = new File(PathToBackup, "backup.img");
	private static File fRECOVERY;
	TextView tvPath;
	ListView lvFiles;
	private static String SYSTEM;
	private static CheckBox cbUseBinary;
	private static boolean firstrun;
	
	Context context = this;
	Support s = new Support();
	FlashUtil fu = new FlashUtil(context);
	FileChooser fc;
	NotificationUtil nu = new NotificationUtil(context);
	CommonUtil cu = new CommonUtil(context);
	Dialog dialog;
	AlertDialog.Builder abuilder;
//	"Methods" need a input from user (AlertDialog) or at the end of AsyncTask
	Runnable rFlash = new Runnable() {

		@Override
		public void run() {
			fu.flash(fRECOVERY);
			nu.createDialog(R.string.info, R.string.flashed, true, true);
		}
		
	};
	Runnable rFlasher = new Runnable(){
		@Override
		public void run() {
			try{
			if (fc.use){
				fRECOVERY = fc.selectedFile;
			}} catch (Exception e) {}
			if (fRECOVERY.exists()){
				if (!s.KERNEL_TO 
						&& !s.FLASH_OVER_RECOVERY) {
					rFlash.run();
				} else {
					if (s.KERNEL_TO)
						nu.createAlertDialog(R.string.warning, R.string.kernel_to, rFlash);
					if (s.FLASH_OVER_RECOVERY) {
						abuilder = new AlertDialog.Builder(context);
						abuilder
							.setTitle(R.string.info)
							.setMessage(R.string.flash_over_recovery)
							.setPositiveButton(R.string.positive, new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									cu.executeShell("reboot recovery", true);
								}
							})
							.setNeutralButton("Instructions", new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									Dialog d = new Dialog(context);
									d.setTitle("Instructions");
									TextView tv = new TextView(context);
									tv.setTextSize(20);
									tv.setText(R.string.instructions);
									d.setContentView(tv);
									d.setOnCancelListener(new DialogInterface.OnCancelListener() {
										
										@Override
										public void onCancel(DialogInterface dialog) {
											abuilder.show();
											
										}
									});
									d.show();
								}
							})
							.show();
					}
				}
					
			} else {
				nu.createAlertDialog(R.string.info, R.string.getdownload, rDownload);
			}
			
		}
	};
	Runnable rBackup = new Runnable(){
		@Override
		public void run() {
			if (s.FLASH_OVER_RECOVERY 
					|| s.BLM){
				nu.createDialog(R.string.warning, R.string.no_function, true, true);	
			} else {
				fu.backup();
				nu.createDialog(R.string.info, R.string.bakreport, true, true);
			}
		}
	};
	Runnable rRestore = new Runnable(){
		@Override
		public void run() {
			if (s.FLASH_OVER_RECOVERY 
					|| s.BLM){
				nu.createDialog(R.string.warning, R.string.no_function, true, true);	
			} else {
				fu.restore();
				nu.createDialog(R.string.info, R.string.resreport, true, true);
			}
		}
	};
	Runnable rDownload = new Runnable(){
		@Override
		public void run() {
			if (!fRECOVERY.exists()) {
				ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			
				if (networkInfo != null 
						&& networkInfo.isConnected()) {
					downloadFile("http://dslnexus.nazuka.net/recoveries/" + fRECOVERY.getName().toString(), fRECOVERY);
				} else {
					nu.createDialog(R.string.warning, R.string.noconnection, true, true);
				}
			}
		}};
	Runnable runOnTrue = new Runnable() {
		@Override
		public void run() {report();}
		
	};
	Runnable runOnNegative = new Runnable() {

		@Override
		public void run() {
			createNotification(R.drawable.ic_launcher, R.string.warning, R.string.notsupportded, 28);
			finish();
			System.exit(0);
		}
		
	};
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		getSupport(); 
		
		checkFolder();
		
		TextView tvInfo = (TextView) findViewById (R.id.tvInfo);
		
		tvInfo.setText("\nModel: " + android.os.Build.MODEL + "\nName: " + s.DEVICE);
		
		if (!firstrun){
			
			if (!cu.suRecognition()) {
				createNotification(R.drawable.ic_launcher, R.string.warning, R.string.noroot, 28);
				finish();
				System.exit(0);
			}
			
			if (s.BLM){
				cbUseBinary = (CheckBox) findViewById(R.id.cbUseBinary);
				cbUseBinary.setText("Using BLM Flash method");
			}
			
			firstrun = true;
		}
		
		cbUseBinary = (CheckBox) findViewById(R.id.cbUseBinary);
		cbUseBinary.setVisibility(View.VISIBLE);
		cbUseBinary.setChecked(true);
		if (s.RecoveryPath.equals("")
				&& !s.MTD) {
			nu.createAlertDialog(R.string.warning, R.string.notsupportded, true, runOnTrue, false, new Runnable(){public void run() {}}, true, runOnNegative);
		} else if (!s.RecoveryPath.equals("")){
			cbUseBinary.setText(String.format(context.getString(R.string.usedd), "\n" + s.RecoveryPath));
		} else if (s.MTD){
			cbUseBinary.setText(R.string.usebinary);
		}
		
	}

//	Button Methods (onClick)
	@SuppressWarnings("static-access")
	public void Go(View view){
		SYSTEM = view.getTag().toString();
		fRECOVERY = new File(PathToRecoveries, s.DEVICE + "-" + SYSTEM + s.EXT);
		rFlasher.run();
		new Appirater().appLaunched(context);
	}
	
	public void bFlashOther(View view){
		fc = new FileChooser(context, PathToSd.getAbsolutePath(), rFlasher);
	}
	public void bBackup(View view) {
		
		if (fBACKUP.exists()) {
			nu.createAlertDialog(R.string.warning, R.string.backupalready, rBackup);
		} else {
			rBackup.run();
		}
	}
	public void bRestore(View view) {
		if (!fBACKUP.exists()) {
			nu.createAlertDialog(R.string.warning, R.string.nobackup, rBackup);
		} else {
			rRestore.run();
		}
	}
	public void bCleareCache(View view) {
		cu.deleteFolder(PathToRecoveries, false);
	}
	public void bRebooter(View view) {
		new Rebooter(context).run();
	}
//	Called from Button Methods, created to redundancy
	public void downloadFile(String URL, File outputFile) {
		DownloadUtil du = new DownloadUtil(context, URL, outputFile, rFlasher);
		du.execute();
	}

	public void getSupport() {
	
//	TWRP unsupported devices
		if (!s.TWRP) {
			Button bTWRP = (Button) findViewById(R.id.bTWRP);
			bTWRP.setText(R.string.notwrp);
			bTWRP.setClickable(false);
		}

//	CWM unsupported devices
		if (!s.CWM) {
			Button bCWM = (Button) findViewById(R.id.bCWM);
			bCWM.setText(R.string.nocwm);
			bCWM.setClickable(false);
		}
	}
	
	@SuppressWarnings("deprecation")
	public void createNotification(int Icon, int Title, int Message, int nid) {
		Intent intent = new Intent();
		PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification n = new Notification(Icon, getString(Message), System.currentTimeMillis());
		n.setLatestEventInfo(this, getString(Title), getString(Message), pi);
		n.flags = Notification.FLAG_AUTO_CANCEL;
		nm.notify(nid, n);	
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	    return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.iProfile:
	            cu.xdaProfile();
	            return true;
	        case R.id.iReport:
	        	report();
	        	return true;
	        case R.id.iExit:
	        	finish();
	    		System.exit(0);
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	private void checkFolder(){
		cu.checkFolder(PathToRecoveryTools);
		cu.checkFolder(PathToRecoveries);
		cu.checkFolder(PathToBackup);
	}
	
	public void report() {
		dialog = new Dialog(context);
		dialog.setContentView(R.layout.dialog_comment);
		dialog.setTitle("Commentar");
		Button ok = (Button) dialog.findViewById(R.id.bGo);
		ok.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
					EditText text = (EditText) dialog.findViewById(R.id.editText1);
					String comment = text.getText().toString();
					Intent intent = new Intent(Intent.ACTION_SEND);
					intent.setType("text/plain");
					intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"ashotmkrtchyan1995@gmail.com"});
					intent.putExtra(Intent.EXTRA_SUBJECT, "Recovery-Tools report to support new Device");
					intent.putExtra(Intent.EXTRA_TEXT, "Package Infos:" +
							"\n\nName: " + pInfo.packageName +
							"\nVersionName: " + pInfo.versionName +
							"\nVersionCode: " + pInfo.versionCode +
							"\n\n\nProduct Info: " + 
							"\n\nManufacture: " + android.os.Build.MANUFACTURER + 
							"\nDevice: " + android.os.Build.DEVICE + 
							"\nBoard: " + android.os.Build.BOARD + 
							"\nBrand: " + android.os.Build.BRAND +
							"\n\n\n===========Comment==========\n" + comment +
							"\n===========Comment==========");
					startActivity(Intent.createChooser(intent, "Send as EMAIL"));
					dialog.dismiss();
				
				} catch (NameNotFoundException e) {
					nu.createDialog(R.string.warning, e.getMessage(), true);
					e.printStackTrace();
				}
			}
		});
		
		dialog.show();
	}
}