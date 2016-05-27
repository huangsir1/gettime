package com.example.doctorassistant;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.sleep.runningman.CustomProgressDialog_Connected;
import com.sleep.runningman.CustomProgressDialog_Searching;
import com.sleep.runningman.MyCustomDialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
	private String mlmin;
	private String mlmax;
	private String xymin;
	private String xymax;
	private BluetoothAdapter mBluetoothAdapter;
	private MyBluetoothService mMyBluetoothService;
	private SharedPreferences mSharedPreferences;
	private SharedPreferences.Editor mEditor;
	private String bluetooth_mac = null;
	private CustomProgressDialog_Searching dialog_searching;
	private CustomProgressDialog_Connected dialog_connected;
	private Timer t = new Timer();
	private MyCustomDialog dialog_reconnection;
	private int num = 100;
	private int delete_number;
	private String number;
	private String name;
	private Sleep_ApneaDetection mSleep_ApneaDetection;
	public static int past_fileLines = 0;
	MyDatabaseHelper dbHelper;
	ArrayList<Map<String, String>> datalist;
	MyAdapter myAdapter;
	ViewHolder holder = null;
	private String dataPath = Utils.getInnerSDCardPath() + File.separator + "SleepCare" + File.separator + "data"; // 数据的存储路径
	private Sleep_ApneaDetection sad = null;
	private Button bt_deleteAll;
	private Boolean isconnected=false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		bt_deleteAll=(Button) findViewById(R.id.bt_deleteall);
		dbHelper = new MyDatabaseHelper(this, "myhistory.db3", 1);
		ListView listView = (ListView) findViewById(R.id.show_table);
		datalist = getData();
		myAdapter = new MyAdapter(this);
		listView.setAdapter(myAdapter);
		listView.setOnItemLongClickListener(itemLongClickListener);

		mSharedPreferences = this.getSharedPreferences("user", MODE_PRIVATE);
		mEditor = mSharedPreferences.edit();
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mMyBluetoothService = new MyBluetoothService(MainActivity.this, mHandler, mBluetoothAdapter, mHandler1, "11");
		this.registBR();
		this.startBT();
		bt_deleteAll.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialogForDeleteAll();
			}
		});
	}

	@Override
	protected void onStop() {
		super.onStop();
		for (int i = 0; i < datalist.size(); i++) {
			String name = datalist.get(i).get("name");
			String number = String.valueOf(i);
			if (name != null) {
				Cursor cursor = dbHelper.getReadableDatabase().rawQuery("select * from dictt where number like ? ",
						new String[] { number });
				if (cursor.getCount() >= 0) {
					dbHelper.getReadableDatabase().execSQL("update dictt set name=? where number=?",
							new String[] { name, number });
				} else {

				}
			}
		}
	}

	public class MyAdapter extends BaseAdapter {

		private LayoutInflater mInflater;

		public MyAdapter(Context context) {
			this.mInflater = LayoutInflater.from(context);
		}
		@Override
		public int getCount() {
			return datalist.size();
		}
		@Override
		public Object getItem(int position) {
			return null;
		}
		@Override
		public long getItemId(int position) {
			return 0;
		}
		@SuppressLint("InflateParams")
		public View getView(final int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.line, null);
				holder.et_name = (EditText) convertView.findViewById(R.id.et_name);
				holder.et_mlmin = (EditText) convertView.findViewById(R.id.et_mlmin);
				holder.et_ml = (EditText) convertView.findViewById(R.id.et_ml);
				holder.et_mlmax = (EditText) convertView.findViewById(R.id.et_mlmax);
				holder.et_xymin = (EditText) convertView.findViewById(R.id.et_xymin);
				holder.et_xy = (EditText) convertView.findViewById(R.id.et_xy);
				holder.et_xymax = (EditText) convertView.findViewById(R.id.et_xymax);
				holder.bt_history = (Button) convertView.findViewById(R.id.bt_history);
				holder.bt_num = (Button) convertView.findViewById(R.id.bt_num);
				holder.bt_image = (ImageButton) convertView.findViewById(R.id.bt_image);
				holder.bt_delete = (Button) convertView.findViewById(R.id.bt_delete);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.bt_num.setText(String.valueOf(position));
			holder.et_name.setText((String) datalist.get(position).get("name"));
			holder.et_mlmin.setText((String) datalist.get(position).get("mlmin"));
			holder.et_ml.setText((String) datalist.get(position).get("ml"));
			holder.et_mlmax.setText((String) datalist.get(position).get("mlmax"));
			holder.et_xymin.setText((String) datalist.get(position).get("xymin"));
			holder.et_xy.setText((String) datalist.get(position).get("xy"));
			holder.et_xymax.setText((String) datalist.get(position).get("xymax"));
			if (position == num) {
				holder.bt_num.setAlpha(0);
//				Animation anim=AnimationUtils.loadAnimation(MainActivity.this, R.anim.listen);
//				holder.bt_image.startAnimation(anim);
			} else {
				holder.bt_num.setAlpha(1);
			}
			holder.et_name.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_UP) {
						editCustomView(position);
					}
					return false;
				}
			});
			holder.bt_num.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (position!=num) {
						dialogForChange(position);
					}else {
						dialogForChange1(position);
					}
				}
			});
			holder.bt_history.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					String key = String.valueOf(position);
					Cursor cursor = dbHelper.getReadableDatabase().rawQuery("select * from dictt where number like ? ",
							new String[] { key });
					if (cursor.getCount() >= 1) {
						Bundle data = new Bundle();
						data.putSerializable("data", converCursorToList(cursor));
//						cursor.moveToFirst();
//						data.putString("name", cursor.getString(cursor.getColumnIndex("name")));
						data.putString("name", datalist.get(position).get("name").toString());
						data.putInt("rank", position);
						Intent intent = new Intent(MainActivity.this, ResultActivity.class);
						intent.putExtras(data);
						startActivity(intent);
					} else {
						Toast.makeText(MainActivity.this, "此患者暂无历史纪录", Toast.LENGTH_SHORT).show();
					}
				}
			});
			holder.bt_delete.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					delete_number = position;
					dialogForDelete(delete_number);
				}
			});
			return convertView;
		}
	}

	public final class ViewHolder {
		public Button bt_history;
		public Button bt_delete;
		public Button bt_num;
		public ImageButton bt_image;
		public EditText et_name;
		public EditText et_mlmin;
		public EditText et_ml;
		public EditText et_mlmax;
		public EditText et_xymin;
		public EditText et_xy;
		public EditText et_xymax;
	}

	private OnItemLongClickListener itemLongClickListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			editCustomView(position);
			return false;
		}

	};

	public void editCustomView(int num) {
		final int position = num;
		TableLayout loginForm = (TableLayout) getLayoutInflater().inflate(R.layout.login, null);
		final EditText et_name = (EditText) loginForm.findViewById(R.id.name);
		new AlertDialog.Builder(this).setIcon(R.drawable.tools).setTitle("编辑患者资料").setView(loginForm)
				.setPositiveButton("确认保存", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Map<String, String> map = new HashMap<String, String>();
						String name = et_name.getText().toString();
						mlmin = (String) datalist.get(position).get("mlmin");
						mlmax = (String) datalist.get(position).get("mlmax");
						xymin = (String) datalist.get(position).get("xymin");
						xymax = (String) datalist.get(position).get("xymax");
						map.put("name", name);
						map.put("mlmin", mlmin);
						map.put("mlmax", mlmax);
						map.put("xymin", xymin);
						map.put("xymax", xymax);
						datalist.set(position, map);
					}
				}).setNegativeButton("取消修改", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}).create().show();
	}

	// wanghanqing
	private void startBT() {
		// mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		bluetooth_mac = mSharedPreferences.getString("bluetooth_mac", null);
		if (mBluetoothAdapter == null) {
			Log.d(Utils.TAG, "mBluetoothAdapter == null");
			Toast.makeText(MainActivity.this, "由于未知错误，未能开启蓝牙功能！", Toast.LENGTH_SHORT).show();
			// mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			// this.startBT();
			return;
		}
		if (!mBluetoothAdapter.isEnabled()) {
			Log.d(Utils.TAG, "!mBluetoothAdapter.isEnabled()");
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, Utils.BLUETOOTH__REQUEST_ENABLE);
		} else if (bluetooth_mac != null) {
			Log.d(Utils.TAG, "bluetooth_mac != null");
			// 显示蓝牙搜索动画
			mHandler.obtainMessage(Utils.TOAST_BLUETOOTH, Utils.BT_CONNECTING, -1).sendToTarget();
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(bluetooth_mac);
			if (mMyBluetoothService != null) {
				mMyBluetoothService.connect(device);
			} else {
				mMyBluetoothService = new MyBluetoothService(MainActivity.this, mHandler, mBluetoothAdapter, mHandler1,
						"11");
				mMyBluetoothService.connect(device);
			}
		} else if (bluetooth_mac == null) {
			Log.d(Utils.TAG, "mBluetoothAdapter.startDiscovery();");
			mBluetoothAdapter.startDiscovery();
		}
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case Utils.DELIVER_RESULTS:
				HashMap<String, Object> map = (HashMap<String, Object>) msg.obj;
				if (map != null) {
					mSleep_ApneaDetection = (Sleep_ApneaDetection) map.get("sad");
					Integer num_temp = (Integer) map.get("ischange");
					if (mSleep_ApneaDetection != null&& num!=100) {
						int mlmax_temp=mSleep_ApneaDetection.getOutput_max_pr();
						int mlmin_temp=mSleep_ApneaDetection.getOutput_min_pr();
						int xymax_temp=mSleep_ApneaDetection.getOutput_max_spo2();
						int xymin_temp=mSleep_ApneaDetection.getOutput_min_spo2();
						mlmax = String.valueOf(mlmax_temp);
						mlmin = String.valueOf(mlmin_temp);
						xymax = String.valueOf(xymax_temp);
						xymin = String.valueOf(xymin_temp);
						String time = mSleep_ApneaDetection.getOutput_time();
						number = String.valueOf(num);
						name = (String) datalist.get(num).get("name");
						rankInsertData(dbHelper.getReadableDatabase(), name, number, time, mlmin, mlmax, xymin, xymax);
						Map<String, String> map1 = new HashMap<String, String>();
						map1.put("name", name);
						map1.put("mlmin", mlmin);
						map1.put("ml", "");
						map1.put("mlmax", mlmax);
						map1.put("xymin", xymin);
						map1.put("xy", "");
						map1.put("xymax", xymax);
						datalist.set(num, map1);
						myAdapter.notifyDataSetChanged();
						if (num_temp != null) {
							num = num_temp;
						}
						if (xymin_temp<80) {
							Toast.makeText(MainActivity.this, "   血氧饱和度过低！\n请确认是否佩戴好仪器", Toast.LENGTH_LONG).show();
						}
						if (mlmin_temp<45) {
							Toast.makeText(MainActivity.this, "   脉率过低！\n请确认是否佩戴好仪器", Toast.LENGTH_LONG).show();
						}
						if (mlmax_temp>110) {
							Toast.makeText(MainActivity.this, "         脉率过高！\n请确认是否为平静状态，或是否佩戴好仪器", Toast.LENGTH_LONG).show();
						}
					}
				}
				break;
			case Utils.TOAST_BLUETOOTH:
				if (msg.arg1 == Utils.BT_CONNECTING) {
					Log.d(Utils.TAG, "蓝牙设备正在搜索中！");
					// 开始搜索蓝牙
					showDialog_searching(); // 显示动画
				} else if (msg.arg1 == Utils.BT_CONNECT_SUCCESS) {
					Log.d(Utils.TAG, "蓝牙设备连接成功！");
					// socket连接成功
					Toast.makeText(MainActivity.this, "蓝牙设备连接成功！", Toast.LENGTH_SHORT).show();
					if (dialog_searching != null) {
						dialog_searching.dismiss();
						showDialog_connected();
					}
				} else if (msg.arg1 == Utils.BT_CONNECT_FAILED) {
					Log.d(Utils.TAG, "蓝牙设备连接失败，请确认蓝牙设备已开启！");
					// socket连接失败
					if (dialog_searching != null) {
						Log.d(Utils.TAG, "弹出重连窗口2");
						dialog_searching.dismiss();
						showDialog_reconnection();
					}
				}
				break;
			case Utils.TOAST_COMPUTATION:
				if (msg.arg1 == Utils.COMPUTATION_TOO_SHORT) {
					Log.d(Utils.TAG, "测量时间太短！");
					// 测量时间少于3分钟
					Toast.makeText(MainActivity.this, "测量时间太短！\n 请测满20秒", Toast.LENGTH_SHORT).show();
				} else if (msg.arg1 == Utils.COMPUTATION_FINISH) {
					Log.d(Utils.TAG, "数据处理完毕！");
					Toast.makeText(MainActivity.this, "数据处理完毕！", Toast.LENGTH_SHORT).show();
				}
				if (msg.arg1 == Utils.COMPUTATION_DUE_TO_NORMAL) {
					Log.d(Utils.TAG, "测量结束，参数计算中！");
					Toast.makeText(MainActivity.this, "测量结束，参数计算中！", Toast.LENGTH_SHORT).show();
				} else if (msg.arg1 == Utils.COMPUTATION_DUE_TO_ERROR) {
					Log.d(Utils.TAG, "设备或蓝牙异常，已终止测量！");
					Toast.makeText(MainActivity.this, "设备或蓝牙异常，已终止测量！", Toast.LENGTH_SHORT).show();
				}
				break;
			case Utils.TOAST_MEASURE_REEOR:
//				mBluetoothAdapter.startDiscovery();
				break;
			case Utils.SAVE_BLUETOOTH_MAC:
				Log.d(Utils.TAG, "mHandler, SAVE_BLUETOOTH_MAC");
				mEditor.putString("bluetooth_mac", (String) msg.obj).commit();
				break;
			case Utils.ISCALCULATED:

				break;
			case Utils.ISCALCULATED_COMPUTATION:

				break;
			}
		}
	};
	public Handler mHandler1 = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case Utils.SHOW_DATA:
				if (((EquipmentData) msg.obj).getMl() != 255 && ((EquipmentData) msg.obj).getXy() != 127&&num!=100) {
					Map<String, String> map = new HashMap<String, String>();
					map.put("name", (String) datalist.get(num).get("name"));
					map.put("mlmin", "");
					map.put("ml", String.valueOf(((EquipmentData) msg.obj).getMl()));
					map.put("mlmax", "");
					map.put("xymin", "");
					map.put("xy", String.valueOf(((EquipmentData) msg.obj).getXy()));
					map.put("xymax", "");
					datalist.set(num, map);
					myAdapter.notifyDataSetChanged();
				}
				break;
			case Utils.CHRONOMETER:

				break;
			case Utils.RESET_DATA:

				break;
			}
		}
	};

	/**
	 * 显示对话框，提示搜索设备
	 * 
	 * @param v
	 */
	public void showDialog_searching() {
		if (dialog_searching == null) {
			dialog_searching = new CustomProgressDialog_Searching(this, "搜索设备", R.anim.frame);
			dialog_searching.setCancelable(false);
		}
		if (!dialog_searching.isShowing()) {
			t.schedule(new TimerTask() {
				public void run() {
					if (dialog_searching != null && dialog_searching.isShowing())
						dialog_searching.dismiss(); // when the task active then
													// close the
													// dialog_searching
					mBluetoothAdapter.cancelDiscovery();
				}
			}, 20000); // after 20 second (or 20000 miliseconds), the task will
						// be
						// active.
			dialog_searching.show();
		} else {
			Log.d(Utils.TAG, "dialog_searching.isShowing()");
		}
	}

	/**
	 * 显示对话框，提示设备已连接
	 * 
	 * @param v
	 */
	public void showDialog_connected() {
		if (dialog_connected == null) {
			dialog_connected = new CustomProgressDialog_Connected(this);
			dialog_connected.setCancelable(false);
		}
		if (dialog_connected != null && !dialog_connected.isShowing()) {
			dialog_connected.show();
			t.schedule(new TimerTask() {
				public void run() {
					if (dialog_connected != null && dialog_connected.isShowing())
						dialog_connected.dismiss(); // when the task active then
													// close the
													// dialog_searching
				}
			}, 2000); // after 2 second (or 2000 miliseconds), the task will be
						// active.
		} else {
			Log.d(Utils.TAG, "dialog_connected.isShowing()");
		}
	}

	/**
	 * 显示对话框，提示重新搜索蓝牙设备
	 * 
	 * @param v
	 */
	public void showDialog_reconnection() {
		if (dialog_reconnection == null) {
			dialog_reconnection = new MyCustomDialog(MainActivity.this, "           蓝牙设备连接失败，\n请确认蓝牙设备开启后重新连接！", true,
					"重新搜索", new MyCustomDialog.OnCustomDialogListener() {
						@Override
						public void callback() {
							mBluetoothAdapter.startDiscovery();
						}
					});
		}
		dialog_reconnection.show();
		Log.d(Utils.TAG, "弹出重连窗口3");
	}

	// wanghanqing
	private void registBR() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		registerReceiver(mBroadcastReceiver, filter);
	}

	// wanghanqing
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null)
				return;
			String action = intent.getAction();

			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				Log.d(Utils.TAG, "BluetoothDevice.ACTION_FOUND");
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (device != null && device.getName() != null && Utils.DEVICE_NAME.equals(device.getName())) { // 蓝牙设备搜索时，筛选出名字为"BerryMed"的设备
					if (mMyBluetoothService != null) {
						mMyBluetoothService.connect(device);
						past_fileLines=0;
						isconnected=true;
					} else {
						mMyBluetoothService = new MyBluetoothService(MainActivity.this, mHandler, mBluetoothAdapter,
								mHandler1, "11");
						mMyBluetoothService.connect(device);
						past_fileLines=0;
						isconnected=true;
					}
				}
			} else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
				
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				Log.d(Utils.TAG, "BluetoothDevice.ACTION_DISCOVERY_FINISHED");
				if (mMyBluetoothService == null
						|| (mMyBluetoothService != null && !mMyBluetoothService.bluetooth_connecting)) {
					Log.d(Utils.TAG, "弹出重连窗口1");
					mHandler.obtainMessage(Utils.TOAST_BLUETOOTH, Utils.BT_CONNECT_FAILED, -1).sendToTarget();
				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				Log.d(Utils.TAG, "BluetoothDevice.ACTION_DISCOVERY_STARTED");
				// 显示蓝牙搜索动画
				mHandler.obtainMessage(Utils.TOAST_BLUETOOTH, Utils.BT_CONNECTING, -1).sendToTarget();
			}
		}
	};

	/**
	 * 移除所有正在显示的dialog
	 */
	public void dismissDialog() {
		if (dialog_searching != null && dialog_searching.isShowing()) {
			dialog_searching.dismiss();
		}
		if (dialog_connected != null && dialog_connected.isShowing()) {
			dialog_connected.dismiss();
		}
		if (dialog_reconnection != null) {
			dialog_reconnection.dismiss();
		}
	}

	public void onDestroy() {
		super.onDestroy();
		Log.d(Utils.TAG, "MainActivity, onDestroy()");
		dismissDialog();
		if (mHandler != null)
			mHandler.removeCallbacksAndMessages(null);
		if (mMyBluetoothService != null)
			mMyBluetoothService.killDataThread();
		// Make sure we're not doing discovery anymore
		if (mBluetoothAdapter != null) {
			mBluetoothAdapter.cancelDiscovery();
		}
		// Unregister broadcast listeners
		if (mBroadcastReceiver != null) {
			MainActivity.this.unregisterReceiver(mBroadcastReceiver);
		}
		if (dbHelper != null) {
			dbHelper.close();
		}
	}

	protected ArrayList<Map<String, String>> converCursorToList(Cursor cursor) {
		ArrayList<Map<String, String>> result = new ArrayList<Map<String, String>>();
		while (cursor.moveToNext()) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("time", cursor.getString(cursor.getColumnIndex("time")));
			map.put("mlmin", cursor.getString(cursor.getColumnIndex("mlmin")));
			map.put("mlmax", cursor.getString(cursor.getColumnIndex("mlmax")));
			map.put("xymin", cursor.getString(cursor.getColumnIndex("xymin")));
			map.put("xymax", cursor.getString(cursor.getColumnIndex("xymax")));
			result.add(map);
			Map<String, String> map1 = new HashMap<String, String>();
			map1.put("time", cursor.getString(cursor.getColumnIndex("time1")));
			map1.put("mlmin", cursor.getString(cursor.getColumnIndex("mlmin1")));
			map1.put("mlmax", cursor.getString(cursor.getColumnIndex("mlmax1")));
			map1.put("xymin", cursor.getString(cursor.getColumnIndex("xymin1")));
			map1.put("xymax", cursor.getString(cursor.getColumnIndex("xymax1")));
			result.add(map1);
			Map<String, String> map2 = new HashMap<String, String>();
			map2.put("time", cursor.getString(cursor.getColumnIndex("time2")));
			map2.put("mlmin", cursor.getString(cursor.getColumnIndex("mlmin2")));
			map2.put("mlmax", cursor.getString(cursor.getColumnIndex("mlmax2")));
			map2.put("xymin", cursor.getString(cursor.getColumnIndex("xymin2")));
			map2.put("xymax", cursor.getString(cursor.getColumnIndex("xymax2")));
			result.add(map2);
			Map<String, String> map3 = new HashMap<String, String>();
			map3.put("time", cursor.getString(cursor.getColumnIndex("time3")));
			map3.put("mlmin", cursor.getString(cursor.getColumnIndex("mlmin3")));
			map3.put("mlmax", cursor.getString(cursor.getColumnIndex("mlmax3")));
			map3.put("xymin", cursor.getString(cursor.getColumnIndex("xymin3")));
			map3.put("xymax", cursor.getString(cursor.getColumnIndex("xymax3")));
			result.add(map3);
		}
		return result;
	}
	/**
	 * 自动循环插入历史数据，若数据超过四组，则从头开始将前面的数据覆盖掉
	 * 
	 * @param number 患者编号
	 */
	private void rankInsertData(SQLiteDatabase db, String name, String number, String time, String mlmin, String mlmax,
			String xymin, String xymax) {
		Cursor cursor = db.rawQuery("select * from dictt where number like ? ", new String[] { number });
		if (cursor.getCount() >= 1) {
			cursor.moveToNext();
			String history_number = cursor.getString(cursor.getColumnIndex("history_number"));
			int history = Integer.parseInt(history_number);
			switch (history) {
			case 0:
				insertData(db, name, number, "1", time, mlmin, mlmax, xymin, xymax);
				break;
			case 1:
				insertData(db, name, number, "2", time, mlmin, mlmax, xymin, xymax);
				break;
			case 2:
				insertData(db, name, number, "3", time, mlmin, mlmax, xymin, xymax);
				break;
			case 3:
				insertData(db, name, number, "0", time, mlmin, mlmax, xymin, xymax);
				break;
			}
		} else {
			insertData(db, name, number, "0", time, mlmin, mlmax, xymin, xymax);
		}
	}

	private void insertData(SQLiteDatabase db, String name, String number, String history_number, String time,
			String mlmin, String mlmax, String xymin, String xymax) {
		Cursor cursor = db.rawQuery("select * from dictt where number like ? ", new String[] { number });
		if (cursor.getCount() == 0&&"0".equals(history_number)) {
			db.execSQL(
					"insert into dictt values(null ,?, ?, ?" + ",?, ? , ? ,? ,?" + ",?, ? , ? ,? ,?,"
							+ "?, ? , ? ,? ,?," + "?, ? , ? ,? ,?)",
					new String[] { name, number, history_number, time, mlmin, mlmax, xymin, xymax, "----:--:--   --:--:--", "--",
							"--", "--", "--", "----:--:--   --:--:--", "--", "--", "--", "--", "----:--:--   --:--:--", "--", "--", "--", "--" });
		} else { // 查询结果不为空就改为更新操作
			cursor.moveToNext();
			if ("1".equals(history_number)) {
				db.execSQL(
						"update dictt set name=?,history_number=?,time1=?,mlmin1=?,mlmax1=?,xymin1=?,xymax1=? where number=?",
						new String[] { name, "1", time, mlmin, mlmax, xymin, xymax, number });
			}
			if ("2".equals(history_number)) {
				db.execSQL(
						"update dictt set name=?,history_number=?,time2=?,mlmin2=?,mlmax2=?,xymin2=?,xymax2=? where number=?",
						new String[] { name, "2", time, mlmin, mlmax, xymin, xymax, number });
			}
			if ("3".equals(history_number)) {
				db.execSQL(
						"update dictt set name=?,history_number=?,time3=?,mlmin3=?,mlmax3=?,xymin3=?,xymax3=? where number=?",
						new String[] { name, "3", time, mlmin, mlmax, xymin, xymax, number });
			}
			if ("0".equals(history_number)) {
				db.execSQL(
						"update dictt set name=?,history_number=?,time=?,mlmin=?,mlmax=?,xymin=?,xymax=? where number=?",
						new String[] { name, "0", time, mlmin, mlmax, xymin, xymax, number });
			}
		}
	}

	public void deleteData(SQLiteDatabase db, String number) {
		db.execSQL("delete from dictt where number like ?", new String[] { number });
		int number_temp = Integer.valueOf(number);
		Map<String, String> map = new HashMap<String, String>();
		map.put("name", "");
		map.put("mlmin", "");
		map.put("mlmax", "");
		map.put("xymin", "");
		map.put("xymax", "");
		datalist.set(number_temp, map);
		myAdapter.notifyDataSetChanged();
		Toast.makeText(MainActivity.this, "此患者数据已清除", Toast.LENGTH_LONG).show();
	}
	public void deleteDataAll(SQLiteDatabase db) {
		db.execSQL("delete from dictt ");
		for (int i = 0; i < 100; i++) {
			Map<String, String> map = new HashMap<String, String>();
				map.put("name", "");
				map.put("mlmin", "");
				map.put("ml", "");
				map.put("mlmax", "");
				map.put("xymin", "");
				map.put("xy", "");
				map.put("xymax", "");
				datalist.set(i, map);
		}
		myAdapter.notifyDataSetChanged();
		Toast.makeText(MainActivity.this, "所有患者数据已清除", Toast.LENGTH_LONG).show();
	}
	public void dialogForChange(final int position) {

		ImageView img = new ImageView(MainActivity.this);
		img.setImageResource(R.drawable.emoji_1f4cc);
		new AlertDialog.Builder(this).setView(img).setTitle("切换警告").setMessage("确定用户已经切换，并戴好设备吗？")
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(num==100){
							num=position;
							Map<String, String> map = new HashMap<String, String>();
							map.put("name", datalist.get(num).get("name"));
							map.put("mlmin", datalist.get(num).get("mlmin"));
							map.put("ml", "");
							map.put("mlmax", datalist.get(num).get("mlmax"));
							map.put("xymin", datalist.get(num).get("xymin"));
							map.put("xy", "");
							map.put("xymax", datalist.get(num).get("xymax"));
							datalist.set(num, map);
							myAdapter.notifyDataSetChanged();
							if (!mMyBluetoothService.bluetooth_connecting) {//如果断开了蓝牙连接
								mBluetoothAdapter.startDiscovery();
							}
						}else {
							Log.e("111", ""+past_fileLines);
							Log.e("111", ""+mMyBluetoothService.socket.isConnected());
							if (!mMyBluetoothService.bluetooth_connecting) {//如果断开了蓝牙连接
								Map<String, String> map = new HashMap<String, String>();
								map.put("name", datalist.get(num).get("name"));
								map.put("mlmin", datalist.get(num).get("mlmin"));
								map.put("ml", "");
								map.put("mlmax", datalist.get(num).get("mlmax"));
								map.put("xymin", datalist.get(num).get("xymin"));
								map.put("xy", "");
								map.put("xymax", datalist.get(num).get("xymax"));
								datalist.set(num, map);
								num=position;
								myAdapter.notifyDataSetChanged();
								mBluetoothAdapter.startDiscovery();
							}else {                                //若为连接状态
								isconnected=true;
								Boolean flag = caculate(position);
								if (flag) {
									Map<String, String> map = new HashMap<String, String>();
									name = (String) datalist.get(position).get("name");
									map.put("name", name);
									map.put("mlmin", "");
									map.put("ml", "");
									map.put("mlmax", "");
									map.put("xymin", "");
									map.put("xy", "");
									map.put("xymax", "");
									datalist.set(position, map);
									myAdapter.notifyDataSetChanged();
								}
							}
						}
					}
				}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
	}
	public void dialogForChange1(final int position) {

		ImageView img = new ImageView(MainActivity.this);
		img.setImageResource(R.drawable.emoji_1f4cc);
		new AlertDialog.Builder(this).setView(img).setTitle("增加测量").setMessage("确定要继续测量该患者吗？")
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(num==100){
							num=position;
							Map<String, String> map = new HashMap<String, String>();
							map.put("name", datalist.get(num).get("name"));
							map.put("mlmin", datalist.get(num).get("mlmin"));
							map.put("ml", "");
							map.put("mlmax", datalist.get(num).get("mlmax"));
							map.put("xymin", datalist.get(num).get("xymin"));
							map.put("xy", "");
							map.put("xymax", datalist.get(num).get("xymax"));
							datalist.set(num, map);
							myAdapter.notifyDataSetChanged();
							if (!mMyBluetoothService.bluetooth_connecting) {//如果断开了蓝牙连接
								mBluetoothAdapter.startDiscovery();
							}
						}else {
							Log.e("111", ""+past_fileLines);
							Log.e("111", ""+mMyBluetoothService.socket.isConnected());
							if (!mMyBluetoothService.bluetooth_connecting) {//如果断开了蓝牙连接
								Map<String, String> map = new HashMap<String, String>();
								map.put("name", datalist.get(num).get("name"));
								map.put("mlmin", datalist.get(num).get("mlmin"));
								map.put("ml", "");
								map.put("mlmax", datalist.get(num).get("mlmax"));
								map.put("xymin", datalist.get(num).get("xymin"));
								map.put("xy", "");
								map.put("xymax", datalist.get(num).get("xymax"));
								datalist.set(num, map);
								num=position;
								myAdapter.notifyDataSetChanged();
								mBluetoothAdapter.startDiscovery();
							}else {                                //若为连接状态
								isconnected=true;
								Boolean flag = caculate(position);
								if (flag) {
									Map<String, String> map = new HashMap<String, String>();
									name = (String) datalist.get(position).get("name");
									map.put("name", name);
									map.put("mlmin", "");
									map.put("ml", "");
									map.put("mlmax", "");
									map.put("xymin", "");
									map.put("xy", "");
									map.put("xymax", "");
									datalist.set(position, map);
									myAdapter.notifyDataSetChanged();
								}
							}
						}
					}
				}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
	}
	protected boolean caculate(int position) {                 //时间足够就计算结果，时间不够就提示时间太短
		String filepath = dataPath + File.separator + "11_" + "sleepdata_temp.txt";
		int count_new = Utils.lineCountTotal(filepath);
		int time_last = count_new - past_fileLines;
		if (time_last > 20) { // 设定测量最短时间，约20s
			sad = new Sleep_ApneaDetection();
			sad.calculate(filepath, true, false, Utils.DATA_SEPARATOR, past_fileLines);
			HashMap<String, Object> map_data = new HashMap<String, Object>();
			map_data.put("sad", sad);
			if (num != position) {
				map_data.put("ischange", position);
			}
			mHandler.obtainMessage(Utils.DELIVER_RESULTS, map_data).sendToTarget();
			past_fileLines = Utils.lineCountTotal(filepath);
			return true;
		} else {
			mHandler.obtainMessage(Utils.TOAST_COMPUTATION, Utils.COMPUTATION_TOO_SHORT, -1).sendToTarget(); // 弹出toast，数据太短
			return false;
		}
	}

	public void dialogForDelete(int position) {

		ImageView img = new ImageView(MainActivity.this);
		img.setImageResource(R.drawable.emoji_26a0);
		new AlertDialog.Builder(this).setView(img).setTitle("删除警告")
				.setMessage("确定要删除序号为" + position + "的患者所有的数据吗\n删除后将无法恢复")
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						deleteData(dbHelper.getReadableDatabase(), String.valueOf(delete_number));
					}
				}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
	}
	public void dialogForDeleteAll() {

		ImageView img = new ImageView(MainActivity.this);
		img.setImageResource(R.drawable.emoji_26a0);
		new AlertDialog.Builder(this).setView(img).setTitle("删除警告")
				.setMessage("确定要删除所有患者的数据吗\n删除后将无法恢复")
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						deleteDataAll(dbHelper.getReadableDatabase());
					}
				}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
	}

	private ArrayList<Map<String, String>> getData() {
		ArrayList<Map<String, String>> data = new ArrayList<Map<String, String>>();
		for (int i = 0; i < 100; i++) {
			Map<String, String> map = new HashMap<String, String>();
			String key = String.valueOf(i);
			Cursor cursor = dbHelper.getReadableDatabase().rawQuery("select * from dictt where number like ? ",
					new String[] { key });
			if (cursor.getCount() >= 1) {
				cursor.moveToNext();
				String history_num = cursor.getString(cursor.getColumnIndex("history_number"));
				int hist_num = Integer.valueOf(history_num);
				map.put("name", cursor.getString(cursor.getColumnIndex("name")));
				map.put("mlmin", cursor.getString(5 + hist_num * 5));
				map.put("ml", "");
				map.put("mlmax", cursor.getString(6 + hist_num * 5));
				map.put("xymin", cursor.getString(7 + hist_num * 5));
				map.put("xy", "");
				map.put("xymax", cursor.getString(8 + hist_num * 5));
			} else {
				map.put("name", "");
				map.put("mlmin", "");
				map.put("ml", "");
				map.put("mlmax", "");
				map.put("xymin", "");
				map.put("xy", "");
				map.put("xymax", "");
			}
			data.add(map);
		}
		return data;
	}
}
