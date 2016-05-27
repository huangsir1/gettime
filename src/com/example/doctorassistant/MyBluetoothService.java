package com.example.doctorassistant;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import com.example.doctorassistant.MyDataUtil.NewDataNotify;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class MyBluetoothService implements NewDataNotify {

	private Context mContext;
	private Handler showToastHandler; // MainActivity
	private Handler showDataHandler; // MonitorFragment
	private Handler mDataHandler; // 本类内部的
	private BluetoothAdapter mBluetoothAdapter = null;
	private ConnectThread mConnectThread = null;
	private DataReadThread mDataReadThread = null;
	private Thread mDataThread = null;
	private MyDataUtil mMyDataUtil = null;
	private Sleep_ApneaDetection sad = null; // 呼吸指标计算类
	private Sleep_stageSeperate sss = null; // 睡眠分期指标计算类

	private int finish_count = 0; // 判断测量终点的计数器
	private boolean finish_flag = false; // 辅助发送测量结束信息，true：测量结束，false：测量未结束
	private EquipmentData mEquipmentData = null; // 定时存储测量数据的数据源
	private Timer saveData_1s = null; // 定时存储测量数据的定时器
	private String time_err = null; // 判断设备异常关闭字符串
	private int count_err = 0; // 判断设备异常关闭的计数器
	public boolean bluetooth_connecting = false; // 表示是否已经连接成功或者正在连接蓝牙设备
	private boolean timecounter_flag = false; // 表示计时器的状态，true：计时，false：停止
	public boolean calculating = false; // 表示计算的状态，true：计算未结束，false：计算已结束
	public int temp_fileLines = 0; // 每次由于设备脱落引起计算时，保存文件内数据的行数，用于设备关闭引起计算时进行对比，若文件行数未发生变化，则跳过计算

	private File f_SleepData_status; // 用于分析的数据，根据status存储
	private FileWriter fw_SleepData_status;
	private BufferedWriter bw_SleepData_status;
	private File f_SleepData_1s; // 用于分析的数据，每秒存一次
	private FileWriter fw_SleepData_1s;
	private BufferedWriter bw_SleepData_1s;
	private File f_sleepdata_temp; // 用于计算的数据
	private FileWriter fw_sleepdata_temp;
	private BufferedWriter bw_sleepdata_temp;
	
	//2016.4.25
	private int status_8_count = 0; // 记录status连续出现8的次数
	private boolean flag_data_error = false; // 判断status是否连续出现8
	private File f_data_error; // 当status连续出现8的情况，保存原始数据
	private FileWriter fw_data_error;
	private BufferedWriter bw_data_error;
	
	@SuppressLint("SimpleDateFormat")
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
	private String dataPath = Utils.getInnerSDCardPath() + File.separator
			+ "SleepCare" + File.separator + "data"; // 数据的存储路径
	private String userName = null;  //用户名
	private String filePath = null;
	public BluetoothSocket socket;

	public MyBluetoothService(Context mContext, Handler showToastHandler,
			BluetoothAdapter mBluetoothAdapter, Handler showDataHandler, String userName) {
		initDataThread();
		this.mContext = mContext;
		this.showToastHandler = showToastHandler;
		this.mBluetoothAdapter = mBluetoothAdapter;
		this.showDataHandler = showDataHandler;
		this.userName = userName;
	}

	/**
	 * 进行蓝牙连接
	 * 
	 * @param device
	 *            需要连接的蓝牙设备
	 */
	public void connect(BluetoothDevice device) {
//		// 关闭仍在运行的线程，对需要用到的线程进行重置
//		if (mDataHandler != null)
//			mDataHandler.sendEmptyMessage(Utils.SHUTDOWN);
		// 开始连接蓝牙
		mConnectThread = new ConnectThread(device);
		bluetooth_connecting = true; // 表示是否已经连接成功或者正在连接蓝牙设备
		if (mBluetoothAdapter.isDiscovering())
			mBluetoothAdapter.cancelDiscovery();
		mConnectThread.start();
	}

	/**
	 * 关闭所有数据接收的线程，关闭数据接收功能
	 */
	public void stop() {
		mDataHandler.sendEmptyMessage(Utils.SHUTDOWN);
	}

	/**
	 * 关闭数据处理Handler所在的线程，彻底关闭蓝牙接收功能
	 */
	public void killDataThread() {
		Log.d(Utils.TAG, "MyBluetoothService, killDataThread()");
		if (mDataThread != null) {
			if (mDataHandler != null) {
				mDataHandler.removeCallbacksAndMessages(null);
			}
			mDataThread.interrupt();
		}
	}
	
	/**
	 * 执行计算
	 */
	public void exeCalculation(){
//	public void exeCalculation(String filepath){
		Log.d(Utils.TAG, "MyBluetoothService, exeCalculation()");
		mDataHandler.sendEmptyMessage(Utils.EXE_COMPUTATION);
//		mDataHandler.obtainMessage(Utils.EXE_COMPUTATION, filepath).sendToTarget();
	}

	/**
	 * 传入BluetoothDevice，获取BluetoothSocket并连接
	 */
	private class ConnectThread extends Thread {

		@SuppressLint("NewApi")
		public ConnectThread(BluetoothDevice device) {

			int sdk = Build.VERSION.SDK_INT;
			if (sdk >= 10) {
				try {
					Log.d(Utils.TAG, "MyBluetoothService, ConnectThread create, socket create***1");
					socket = device
							.createInsecureRfcommSocketToServiceRecord(Utils.COMMON_UUID);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				try {
					Log.d(Utils.TAG, "MyBluetoothService, ConnectThread create, socket create***2");
					socket = device
							.createRfcommSocketToServiceRecord(Utils.COMMON_UUID);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
//			bluetooth_connecting = true;
			showToastHandler.obtainMessage(Utils.SAVE_BLUETOOTH_MAC,
					device.getAddress()).sendToTarget();
		}

		@SuppressLint("NewApi")
		public void run() {
			try {
				socket.connect(); // 蓝牙连接
			} catch (IOException e) {
				Log.e(Utils.TAG,
						"MyBluetoothService, ConnectThread run failed, socket.connect() failed: "
								+ e.toString());
				bluetooth_connecting = false;
				// 弹出toast，蓝牙连接失败
				showToastHandler.obtainMessage(Utils.TOAST_BLUETOOTH,
						Utils.BT_CONNECT_FAILED, -1).sendToTarget();
			}

			if (socket.isConnected()) {
				Log.d(Utils.TAG, "MyBluetoothService, ConnectThread run, socket connect success");

				// 初始化参数
				sad = null; // 呼吸指标计算类
				sss = null; // 睡眠分期指标计算类
				finish_count = 0; // 判断测量终点的计数器
				finish_flag = false; // 辅助发送测量结束信息，true：测量结束，false：测量未结束
				mEquipmentData = null; // 定时存储测量数据的数据源
				saveData_1s = null; // 定时存储测量数据的定时器
				time_err = null; // 判断设备异常关闭字符串
				count_err = 0; // 判断设备异常关闭的计数器
				timecounter_flag = false; // 表示计时器的状态，true：计时，false：停止
				calculating = false; // 表示计算的状态，true：计算未结束，false：计算已结束
				temp_fileLines = 0; // 每次由于设备脱落引起计算时，保存文件内数据的行数，用于设备关闭引起计算时进行对比，若文件行数未发生变化，则跳过计算
				
				// 弹出toast，蓝牙连接成功
				showToastHandler.obtainMessage(Utils.TOAST_BLUETOOTH,
						Utils.BT_CONNECT_SUCCESS, -1).sendToTarget();
				// 创建数据存储文件
				mDataHandler.sendEmptyMessage(Utils.INITIAL_FILES);
				
				if (!mBluetoothAdapter.isEnabled()) {
					// 弹出toast，运行发生错误
					showToastHandler.obtainMessage(Utils.TOAST_MEASURE_REEOR,
							Utils.COMPUTATION_DUE_TO_ERROR, -1).sendToTarget();
					// 关闭数据接收
					mDataHandler.sendEmptyMessage(Utils.SHUTDOWN);
					// 进行计算
					mDataHandler.sendEmptyMessage(Utils.EXE_COMPUTATION);
				}

				saveData_1s = new Timer();
				saveData_1s.scheduleAtFixedRate(new TimerTask() {
					int toastcount = 0;

					@Override
					public void run() {
						if (mEquipmentData != null) {
							// 测量发生异常时的提醒，传感器异常、指端传感器脱落、未检测到脉率信号
							if (toastcount == 0
									&& mEquipmentData.getXy() == 127) {
								if (mEquipmentData.getStatus() == 0x01) {
									Log.d(Utils.TAG, "MyBluetoothService, 弹出toast，传感器异常");
									showToastHandler.obtainMessage(
											Utils.TOAST_MEASURE_REEOR,
											Utils.ERROR_SENSOR_OFF, -1)
											.sendToTarget(); // 弹出toast，传感器异常
								}
								if (mEquipmentData.getStatus() == 0x02) {
									Log.d(Utils.TAG, "MyBluetoothService, 弹出toast，指端传感器脱落");
									showToastHandler.obtainMessage(
											Utils.TOAST_MEASURE_REEOR,
											Utils.ERROR_NO_FINGER, -1)
											.sendToTarget(); // 弹出toast，指端传感器脱落
								}
//								if (mEquipmentData.getStatus() == 0x04) {
//									Log.d(Utils.TAG, "MyBluetoothService, 弹出toast，未检测到脉率信号");
//									showToastHandler.obtainMessage(
//											Utils.TOAST_MEASURE_REEOR,
//											Utils.ERROR_NO_PULSE_SIGNAL, -1)
//											.sendToTarget(); // 弹出toast，未检测到脉率信号
//								}
								toastcount++;
							}
							// else if ((mEquipmentData.getStatus() == 0x01
							// || mEquipmentData.getStatus() == 0x02 ||
							// mEquipmentData
							// .getStatus() == 0x04)
							// && mEquipmentData.getXy() == 127) {
							// toastcount++;
							// if (toastcount >= 10) // 每5秒弹一次toast来提醒
							// toastcount = 0;
							// }
							else if (mEquipmentData.getXy() != 127) {
								toastcount = 0;
							}
							// 根据时间值是否发生改变，判断测量是否中断
							if (String.valueOf(mEquipmentData.getNumTime())
									.equals(time_err)) {
								count_err++;
							} else {
								time_err = String.valueOf(mEquipmentData
										.getNumTime());
								count_err = 0;
							}
							// 若数据的时间超过10秒未发生变化，则测量终止
							if (count_err >=1) {
								Log.d(Utils.TAG, "MyBluetoothService, saveData_1s, count_err > 20s");
								
								String filepath = dataPath + File.separator + userName + "_" + "sleepdata_temp.txt";
								int temp = Utils.lineCountTotal(filepath);
								// 判断文件内数据行数与上次计算时是否相同，若比上次计算时多，则进行计算
								if(temp > temp_fileLines){
									Log.d(Utils.TAG, "temp > temp_fileLines");
									// 重置界面上显示的数值
									showDataHandler
											.sendEmptyMessage(Utils.RESET_DATA);
									// 停止计时并将计时器清零
									showDataHandler.obtainMessage(
											Utils.CHRONOMETER,
											Utils.CHRONOMETER_RESET, -1)
											.sendToTarget();
									timecounter_flag = false;
									// 弹出toast，由于异常终止测量
									showToastHandler.obtainMessage(
											Utils.TOAST_COMPUTATION,
											Utils.COMPUTATION_DUE_TO_ERROR, -1)
											.sendToTarget();
									// 关闭数据接收
									mDataHandler.sendEmptyMessage(Utils.SHUTDOWN);
									// 进行计算
									mDataHandler
											.sendEmptyMessage(Utils.EXE_COMPUTATION);
								}else{
									Log.d(Utils.TAG, "temp <= temp_fileLines");
									// 重置界面上显示的数值
									showDataHandler
											.sendEmptyMessage(Utils.RESET_DATA);
									// 停止计时并将计时器清零
									showDataHandler.obtainMessage(
											Utils.CHRONOMETER,
											Utils.CHRONOMETER_RESET, -1)
											.sendToTarget();
									timecounter_flag = false;
									// 关闭数据接收
									mDataHandler.sendEmptyMessage(Utils.SHUTDOWN);
								}
							}

							// 存储用于分析的数据(每隔一秒存一次数据)
							try {
								bw_SleepData_1s
										.write(String.valueOf(mEquipmentData
												.getMl())
												+ ","
//												+ String.valueOf(mEquipmentData
//														.getMlr())
//												+ ","
												+ String.valueOf(mEquipmentData
														.getXy())
												+ ","
//												+ String.valueOf(mEquipmentData
//														.getXyr())
//												+ ","
												+ String.valueOf(mEquipmentData
														.getPI())
												+ ","
												+ String.valueOf(mEquipmentData
														.getStatus())
												+ ","
												+ String.valueOf(mEquipmentData
														.getRR())
												+ ","
//												+ String.valueOf(mEquipmentData
//														.getTime())
//												+ ","
												+ String.valueOf(mEquipmentData
														.getHead1())
												+ ","
												+ String.valueOf(mEquipmentData
														.getHead2())
												+ ","
//												+ String.valueOf(mEquipmentData
//														.getPPG())
//												+ ","
												+ String.valueOf(mEquipmentData
														.getBattery())
												+ ","
												+ String.valueOf(mEquipmentData
														.getIndex())
												+ ","
												+ String.valueOf(mEquipmentData
														.getNumTime())
												+ ","
												+ String.valueOf(mEquipmentData
														.getCheckSum()));
								bw_SleepData_1s.write("\r\n");
								bw_SleepData_1s.flush();
								fw_SleepData_1s.flush();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}, 0, 1000);

				mDataReadThread = new DataReadThread(socket);
				mDataReadThread.start();
			}
		}

		public void cancel() {
			try {
				Log.d(Utils.TAG, "MyBluetoothService, ConnectThread cancel, socket.close()");
				if (socket != null) {
					socket.close();
				}
			} catch (IOException e) {
				Log.e(Utils.TAG,
						"MyBluetoothService, ConnectThread cancel failed, socket.close() failed: "
								+ e.toString());
			}
		}
	}

	/**
	 * 传入BluetoothSocket，获取InputStream，接收传来的数据
	 */
	private class DataReadThread extends Thread {

		private InputStream inputStream;
		private byte[] buffer = new byte[2048];

		public DataReadThread(BluetoothSocket socket) {
			mMyDataUtil = new MyDataUtil(MyBluetoothService.this);

			try {
				Log.d(Utils.TAG, "MyBluetoothService, DataReadThread create, inputStream ready");
				inputStream = socket.getInputStream();
//				bluetooth_connecting = true;
			} catch (IOException e) {
				Log.e(Utils.TAG,
						"MyBluetoothService, DataReadThread create fail, socket.getInputStream() fail: "
								+ e.toString());
			}
		}

		public void run() {
			Log.d(Utils.TAG, "MyBluetoothService, DataReadThread run, receving data");
			int bytes = 0;
			// 停止计时并将计时器清零
			showDataHandler.obtainMessage(Utils.CHRONOMETER,
					Utils.CHRONOMETER_RESET, -1).sendToTarget();
			// 开始计时
			showDataHandler.obtainMessage(Utils.CHRONOMETER,
					Utils.CHRONOMETER_START, -1).sendToTarget();
			timecounter_flag = true;
			// 测量中如果发生闪退，数据会在下次登录时计算(false表示测量数据未被计算)
			showToastHandler.obtainMessage(Utils.ISCALCULATED, Utils.ISCALCULATED_FALSE, -1).sendToTarget();
			while (true) {
				try {
					// bytes = inputStream.read();
					// if (bytes != -1) {
					// mMyDataUtil.addNewByte((byte) bytes);
					// }

					// Read from the InputStream
					if ((bytes = inputStream.read(buffer)) > 0) {
						mMyDataUtil.getNewWork(buffer, bytes);
						if (bytes >= 2048) {
							Log.i(Utils.TAG, "bytes = " + bytes);
						}
						//2016.4.25
						if(flag_data_error){
							mDataHandler.obtainMessage(Utils.DATA_ERROR, bytes, -1, buffer).sendToTarget();
						}
					}
				} catch (IOException e) {
					showToastHandler.obtainMessage(Utils.TOAST_MEASURE_REEOR,
							Utils.BT_CONNECT_FAILED, -1).sendToTarget();
					bluetooth_connecting=false;
					try {
						Log.d(Utils.TAG,
								"MyBluetoothService, DataReadThread run failed, inputStream.close()");
						if (inputStream != null) {
							inputStream.close();
						}
					} catch (IOException e1) {
						Log.e(Utils.TAG,
								"MyBluetoothService, DataReadThread run failed. inputStream.close() fail: "
										+ e1.toString());
					}
					break;
				}
			}
		}

		public void cancel() {
			try {
				Log.d(Utils.TAG, "MyBluetoothService, DataReadThread cancel, inputStream.close()");
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException e) {
				Log.e(Utils.TAG,
						"MyBluetoothService, DataReadThread cancel fail. inputStream.close() failed: "
								+ e.toString());
			}
		}
	}

	public void initDataThread() {
		mDataThread = new Thread() {
			@SuppressLint("HandlerLeak")
			@Override
			public void run() {
				Looper.prepare();
				mDataHandler = new Handler() {
					byte status;

					@Override
					public void handleMessage(Message msg) {
						super.handleMessage(msg);
						switch (msg.what) {
						case Utils.INITIAL_FILES:
							Log.d(Utils.TAG, "MyBluetoothService, mDataHandler, INITIAL_FILES");
							// 判断路径是否存在，若不存在，则创建路径
							File sleep = new File(dataPath);
							if (!sleep.exists())
								sleep.mkdirs();

							// 获取当前时间
							Date currentDate = new Date();

							filePath = dataPath
									+ File.separator
									 + userName + "_"
									+ sdf.format(currentDate)
									+ "_SleepData_status" + ".txt";
							
							// 生成按status保存数据的文件
							try {
								f_SleepData_status = new File(dataPath
										+ File.separator
										 + userName + "_"
										+ sdf.format(currentDate)
										+ "_SleepData_status" + ".txt");
								fw_SleepData_status = new FileWriter(
										f_SleepData_status);
								bw_SleepData_status = new BufferedWriter(
										fw_SleepData_status);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							// 生成每秒保存一次数据的文件
							try {
								f_SleepData_1s = new File(dataPath
										+ File.separator
										+ userName + "_"
										+ sdf.format(currentDate)
										+ "_SleepData_1s" + ".txt");
								fw_SleepData_1s = new FileWriter(f_SleepData_1s);
								bw_SleepData_1s = new BufferedWriter(
										fw_SleepData_1s);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							// 生成计算参数所需的临时数据存放文件
							try {
								f_sleepdata_temp = new File(dataPath
										+ File.separator
										+ userName + "_"
										+ "sleepdata_temp.txt");
								fw_sleepdata_temp = new FileWriter(
										f_sleepdata_temp);
								bw_sleepdata_temp = new BufferedWriter(
										fw_sleepdata_temp);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							//2016.4.25
							// 异常产生时，数据的存储文件
							try {
								f_data_error = new File(dataPath
										+ File.separator
										+ userName + "_"
										+ "data_error.txt");
								fw_data_error = new FileWriter(
										f_data_error);
								bw_data_error = new BufferedWriter(
										fw_data_error);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							// 分别在三个文件中的第一行存入参数名称
							try {
								bw_SleepData_status.write("Ml" //+ "," + "Mlr"
										+ "," + "Xy" //+ "," + "Xyr"
										+ "," + "PI"
										+ "," + "Status" + "," + "RR" + ","
										//+ "Time" + "," 
										+ "Head1" + ","
										+ "Head2" + "," 
										//+ "PPG" + ","
										+ "Battery" + "," + "Index" + ","
										+ "NumTime" + "," + "CheckSum");
								bw_SleepData_status.write("\r\n");
								bw_SleepData_status.flush();
								fw_SleepData_status.flush();

								bw_SleepData_1s.write("Ml" + "," //+ "Mlr" + ","
										+ "Xy" + "," //+ "Xyr" + "," 
										+ "PI" + ","
										+ "Status" + "," + "RR" //+ "," + "Time"
										+ "," + "Head1" + "," + "Head2" + ","
										//+ "PPG" + "," 
										+ "Battery" + ","
										+ "Index" + "," + "NumTime" + ","
										+ "CheckSum");
								bw_SleepData_1s.write("\r\n");
								bw_SleepData_1s.flush();
								fw_SleepData_1s.flush();

								bw_sleepdata_temp
										.write("Ml" + "," + "Xy" + "," + "RR"
												+ "," //+ "Time" + ","
												+ "NumTime"+ "," + "PI");
								bw_sleepdata_temp.write("\r\n");
								bw_sleepdata_temp.flush();
								fw_sleepdata_temp.flush();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							break;
						case Utils.SAVE_DATA:
							// 界面上实时显示数据
							showDataHandler.obtainMessage(Utils.SHOW_DATA,
									msg.obj).sendToTarget();
							status = ((EquipmentData) msg.obj).getStatus();
							// 存储用于计算的数据(按状态位存储)
							if ((status == 0x08)) {
								// 存储pr,spo2,rr和time
								try {
									bw_sleepdata_temp
											.write(String
													.valueOf(((EquipmentData) msg.obj)
															.getMl())
													+ ","
													+ String.valueOf(((EquipmentData) msg.obj)
															.getXy())
													+ ","
													+ String.valueOf(((EquipmentData) msg.obj)
															.getRR())
													+ ","
//													+ String.valueOf(((EquipmentData) msg.obj)
//															.getTime())
//													+ ","
													+ String.valueOf(((EquipmentData) msg.obj)
															.getNumTime())
													+ ","
													+ String.valueOf(((EquipmentData) msg.obj)
															.getPI()));
									bw_sleepdata_temp.write("\r\n");
									bw_sleepdata_temp.flush();
									fw_sleepdata_temp.flush();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							// 存储用于分析的数据(按状态位存储)
							if ((status != 0x00 && status != 0x01
									&& status != 0x02 && status != 0x04)) {
								try {
									if (bw_SleepData_status != null
											&& fw_SleepData_status != null) {
										bw_SleepData_status
												.write(String
														.valueOf(((EquipmentData) msg.obj)
																.getMl())
														+ ","
//														+ String.valueOf(((EquipmentData) msg.obj)
//																.getMlr())
//														+ ","
														+ String.valueOf(((EquipmentData) msg.obj)
																.getXy())
														+ ","
//														+ String.valueOf(((EquipmentData) msg.obj)
//																.getXyr())
//														+ ","
														+ String.valueOf(((EquipmentData) msg.obj)
																.getPI())
														+ ","
														+ String.valueOf(((EquipmentData) msg.obj)
																.getStatus())
														+ ","
														+ String.valueOf(((EquipmentData) msg.obj)
																.getRR())
														+ ","
//														+ String.valueOf(((EquipmentData) msg.obj)
//																.getTime())
//														+ ","
														+ String.valueOf(((EquipmentData) msg.obj)
																.getHead1())
														+ ","
														+ String.valueOf(((EquipmentData) msg.obj)
																.getHead2())
														+ ","
//														+ String.valueOf(((EquipmentData) msg.obj)
//																.getPPG())
//														+ ","
														+ String.valueOf(((EquipmentData) msg.obj)
																.getBattery())
														+ ","
														+ String.valueOf(((EquipmentData) msg.obj)
																.getIndex())
														+ ","
														+ String.valueOf(((EquipmentData) msg.obj)
																.getNumTime())
														+ ","
														+ String.valueOf(((EquipmentData) msg.obj)
																.getCheckSum()));
										bw_SleepData_status.write("\r\n");
										bw_SleepData_status.flush();
										fw_SleepData_status.flush();
									}
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							break;
						case Utils.SHUTDOWN:
							Log.d(Utils.TAG, "MyBluetoothService, mDataHandler, SHUTDOWN");
							if (saveData_1s != null) {
								saveData_1s.cancel();
								saveData_1s.purge();
							}
							if (mConnectThread != null) {
								mConnectThread.cancel();
								mConnectThread.interrupt();
							}
							if (mDataReadThread != null) {
								mDataReadThread.cancel();
								mDataReadThread.interrupt();
							}
							bluetooth_connecting = false;
							//当停止接受数据时，关闭计数器并重置测量
							showDataHandler.obtainMessage(Utils.CHRONOMETER, Utils.CHRONOMETER_RESET, -1).sendToTarget();
							timecounter_flag = false;
							showDataHandler.sendEmptyMessage(Utils.RESET_DATA);
							break;
						case Utils.EXE_COMPUTATION:
							Log.d(Utils.TAG, "MyBluetoothService, mDataHandler, EXE_COMPUTATION");
							calculating = true;
							String filepath = dataPath + File.separator + userName + "_" + "sleepdata_temp.txt";
							int count_new = Utils.lineCountTotal(filepath);
							int time_last = count_new - MainActivity.past_fileLines;
							if (time_last > 20) { // 设定测量最短时间，1分钟
								sad = new Sleep_ApneaDetection();
								sad.calculate(filepath, true, false, Utils.DATA_SEPARATOR,MainActivity.past_fileLines);
								
								showToastHandler.obtainMessage(
										Utils.TOAST_COMPUTATION,
										Utils.COMPUTATION_FINISH, -1)
										.sendToTarget(); // 弹出toast，计算结束
								HashMap<String, Object> map = new HashMap<String, Object>();
								map.put("sad", sad);
								map.put("ischange", 100);
								showToastHandler.obtainMessage(
										Utils.DELIVER_RESULTS, map)
										.sendToTarget();
							} else {
								showToastHandler.obtainMessage(
										Utils.TOAST_COMPUTATION,
										Utils.COMPUTATION_TOO_SHORT, -1)
										.sendToTarget(); // 弹出toast，数据太短
							}
							count_err = 0;
							finish_flag = false;
							temp_fileLines = Utils.lineCountTotal(filepath);
							break;
						//2016.4.25
//						case Utils.DATA_ERROR:
//							Log.d(Utils.TAG, "MyBluetoothService, mDataHandler, DATA_ERROR");
//							try {
//								for(int i = 0; i < msg.arg1; i++){
//									bw_data_error.write(String.valueOf(((byte[]) msg.obj)[i]));
//								}
//								bw_data_error.write("\r\n");
//								bw_data_error.flush();
//								fw_data_error.flush();
//							} catch (IOException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//							break;
						case Utils.INSERT_SEPARATOR:
							Log.d(Utils.TAG, "MyBluetoothService, mDataHandler, INSERT_SEPARATOR");
							// 在用于计算的文件和用于分析的文件中插入分隔符，分隔两次测量
							try {
								bw_sleepdata_temp.write(String.valueOf(Utils.DATA_SEPARATOR));
								bw_sleepdata_temp.write("\r\n");
								bw_sleepdata_temp.flush();
								fw_sleepdata_temp.flush();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							try {
								bw_SleepData_status.write(String.valueOf(Utils.DATA_SEPARATOR));
								bw_SleepData_status.write("\r\n");
								bw_SleepData_status.flush();
								fw_SleepData_status.flush();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							break;
						}
					}
				};
				Looper.loop();
			}
		};
		mDataThread.start();
	}

	@Override
	public void notifyNewData(EquipmentData equipmentData) {
//		//2016.4.25
//		// status连续出现时进行计数，否则重置计数
//		if((equipmentData.getStatus()) == 0x08){
//			status_8_count++;
//		}else{
//			status_8_count = 0;
//		}
//		// 如果status的值连续出现2次以上，开启异常数据存储，计数被重置时，停止存储
//		if(status_8_count >= 2){
//			flag_data_error = true;
//		}else{
//			flag_data_error = false;
//		}
		
		// 根据status值进行计数，判断测量是否终止
		if ((equipmentData.getStatus()) == 0x01
				|| (equipmentData.getStatus()) == 0x02
				|| (equipmentData.getStatus()) == 0x04) {
			finish_count++;
		} else {
			if((equipmentData.getStatus()) == 0x08){
				if(!timecounter_flag){
					mDataHandler.sendEmptyMessage(Utils.INSERT_SEPARATOR);
					showToastHandler.obtainMessage(Utils.ISCALCULATED, Utils.ISCALCULATED_FALSE, -1).sendToTarget();
					showDataHandler.obtainMessage(Utils.CHRONOMETER, Utils.CHRONOMETER_RESET, -1).sendToTarget();
					showDataHandler.obtainMessage(Utils.CHRONOMETER, Utils.CHRONOMETER_START, -1).sendToTarget();
					timecounter_flag = true;
				}

				finish_count = 0;
			}
		}
		// 测量终止
//		if (finish_count >= 1000) { // 测量终点延时，每秒100个数据，10秒延时
//			if (!finish_flag && finish_count <= 1000) {
//				Log.d(Utils.TAG, "MyBluetoothService, notifyNewData(), finish_count > 10s");
		if (finish_count >= 3) { // 测量终点延时，每秒1个数据，5秒延时
			if (!finish_flag && finish_count <= 10) {
				Log.d(Utils.TAG, "MyBluetoothService, notifyNewData(), finish_count > 10s");
				finish_flag = true;
				// 停止计时并将计时器清零
				showDataHandler.obtainMessage(Utils.CHRONOMETER,
						Utils.CHRONOMETER_RESET, -1).sendToTarget();
				timecounter_flag = false;
				// 弹出toast，测量正常结束
				showToastHandler.obtainMessage(Utils.TOAST_COMPUTATION,
						Utils.COMPUTATION_DUE_TO_NORMAL, -1).sendToTarget();
				// 关闭数据接收
//				mDataHandler.sendEmptyMessage(Utils.SHUTDOWN);
				// 进行计算
				mDataHandler.sendEmptyMessage(Utils.EXE_COMPUTATION);
			}
		}
		// 测量进行中
		if (!finish_flag) {
			// 用于每个1秒存储一次数据
			mEquipmentData = equipmentData;
			// 存储用于计算和分析的数据
			mDataHandler.obtainMessage(Utils.SAVE_DATA, equipmentData)
					.sendToTarget();
		}
	}

	@Override
	public void notifyToomuchData(int flag) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifypackagelost() {
		// TODO Auto-generated method stub

	}

}
