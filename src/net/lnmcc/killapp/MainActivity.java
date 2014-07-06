package net.lnmcc.killapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class MainActivity extends Activity {

	private static final String TAG = "KILL_APP_DEMO";
	private static final int MSG_DATASET_CHANGED = 0;

	Button get_running_apps;
	Button kill_all_apps;
	ListView running_apps_lv;

	ActivityManager am;
	// List<RunningTaskInfo> runningTaskInfos = null;
	List<RunningAppProcessInfo> runningAppProcessInfos = null;
	List<String> whiteList = new ArrayList<String>();

	SimpleAdapter running_apps_adapter = null;
	ArrayList<HashMap<String, String>> pkgNames = new ArrayList<HashMap<String, String>>();

	Object lock = new Object();

	private Handler handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		whiteList.add("net.lnmcc.killapp");
		whiteList.add("com.thunderst.weather");

		handler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MSG_DATASET_CHANGED:
					running_apps_adapter.notifyDataSetChanged();
					break;
				default:
					break;
				}
			};
		};

		am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

		get_running_apps = (Button) findViewById(R.id.get_running_apps_btn);
		get_running_apps.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						getApps();
						handler.sendEmptyMessage(MSG_DATASET_CHANGED);
					}
				}).start();
			}
		});

		running_apps_adapter = new SimpleAdapter(this, pkgNames,
				android.R.layout.simple_list_item_1,
				new String[] { "pkgName" }, new int[] { android.R.id.text1 });
		running_apps_lv = (ListView) findViewById(R.id.running_apps_lv);
		running_apps_lv.setAdapter(running_apps_adapter);

		kill_all_apps = (Button) findViewById(R.id.kill_all_apps_btn);
		kill_all_apps.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						killApps();
						handler.sendEmptyMessage(MSG_DATASET_CHANGED);
					}
				}).start();
			}
		});
	}

	private void killApps() {
		synchronized (lock) {
			if (runningAppProcessInfos == null
					|| runningAppProcessInfos.size() == 0)
				return;
			/*
			 * for (RunningTaskInfo info : runningTaskInfos) { if
			 * (!info.baseActivity.getPackageName().equals(getPackageName())) {
			 * Log.d(TAG, "Will kill " + info.baseActivity.getPackageName());
			 * am.killBackgroundProcesses(info.baseActivity.getPackageName()); }
			 * }
			 */
			for (RunningAppProcessInfo info : runningAppProcessInfos) {
				if (/* info.pid != android.os.Process.myPid() */!whiteList
						.contains(info.processName)) {
					Log.d(TAG, "Will kill " + info.processName);
					android.os.Process.killProcess(info.pid);
					am.killBackgroundProcesses(info.processName);
				}
			}
		}
		getApps();
	}

	private void getApps() {
		synchronized (lock) {
			if (runningAppProcessInfos != null)
				runningAppProcessInfos.clear();

			/*
			 * if (runningTaskInfos != null) runningTaskInfos.clear();
			 */

			pkgNames.clear();

			try {
				runningAppProcessInfos = am.getRunningAppProcesses();
				for (RunningAppProcessInfo info : runningAppProcessInfos) {
					HashMap<String, String> map = new HashMap<String, String>();
					map.put("pkgName", info.processName);
					pkgNames.add(map);
				}
				/*
				 * runningTaskInfos = am.getRunningTasks(1000); for
				 * (RunningTaskInfo info : runningTaskInfos) { HashMap<String,
				 * String> map = new HashMap<String, String>();
				 * map.put("pkgName", info.baseActivity.getPackageName()); //
				 * pkgNames.add(map); }
				 */

			} catch (SecurityException ex) {
				Log.d(TAG, "Permission ERROR !");
				// runningTaskInfos = null;
				runningAppProcessInfos = null;
			}
		}
	}
}
