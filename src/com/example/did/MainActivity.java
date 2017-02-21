package com.example.did;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	// UI相关
	private Button btn_setup;
	private Button btn_clear;
	private Button btn_health;
	private TextView txt_value;
	private TextView txt_density;
	private TextView txt_warning;

	// 数据相关
	private Thread thread = null;
	private final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private String DEVICE_ADDRESS = "98:D3:32:20:64:2D";
	private boolean is_connected = false;

	// 设备相关
	private BluetoothAdapter blt_adapter;
	private BluetoothDevice blt_dev_remote;
	private BluetoothSocket socket = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btn_setup = (Button) findViewById(R.id.btn_main_setup);
		btn_clear = (Button) findViewById(R.id.btn_main_clear);
		btn_health = (Button) findViewById(R.id.btn_main_health);
		txt_value = (TextView) findViewById(R.id.txt_main_value);
		txt_density = (TextView) findViewById(R.id.txt_main_density);
		txt_warning = (TextView) findViewById(R.id.txt_main_warning);

		blt_adapter = BluetoothAdapter.getDefaultAdapter();
		blt_dev_remote = blt_adapter.getRemoteDevice(DEVICE_ADDRESS);

		// "开启蓝牙监控"按钮
		btn_setup.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!is_connected) {
					if (!blt_adapter.isEnabled()) // 打开蓝牙
						blt_adapter.enable();

					// 尝试连接蓝牙模块
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								socket = blt_dev_remote
										.createRfcommSocketToServiceRecord(MY_UUID);
								socket.connect();
								handler.sendEmptyMessage(200);// 连接成功
								is_connected = true;

								// 开启读取线程读取数据
								thread = new Thread(new Runnable() {
									@Override
									public void run() {
										byte[] buffer = new byte[1024]; // 每次读取到的数据
										int bytes; // 每次读取到的字节数
										String content = ""; // 持久保存所有读取到的未处理的数据
										try {
											InputStream input = null;
											input = socket.getInputStream();

											while (true) {
												if ((bytes = input.read(buffer)) > 0) {
													byte[] data = new byte[bytes];
													for (int i = 0; i < bytes; ++i)
														data[i] = buffer[i];

													String string = new String(
															data);// 将每次读取到的字节数据转化成字符串
													content += string;// 当前未处理的数据

													String[] res = content
															.split("\n");
													int length;
													if (string.charAt(string
															.length() - 1) == '\n') {
														content = "";
														length = res.length;
													} else {
														content = res[res.length - 1];
														length = res.length - 1;
													}

													for (int i = 0; i < length; ++i)
														displayMessage(res[i]);
												}
											}
										} catch (IOException e) {
											e.printStackTrace();
										}
									}
								});
								thread.start();
							} catch (IOException e) {
								handler.sendEmptyMessage(400);// 连接失败
							}
						}
					}).start();
				} else {
					Toast.makeText(MainActivity.this, "已与蓝牙设备建立连接，请勿重复操作",
							Toast.LENGTH_SHORT).show();
				}
			}
		});

		// "清除警告"按钮
		btn_clear.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if ("".equals(txt_warning.getText()))
					Toast.makeText(MainActivity.this, "警告已清除",
							Toast.LENGTH_SHORT).show();
				else
					txt_warning.setText("");
			}
		});

		btn_health.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(MainActivity.this, "该功能暂未开发", Toast.LENGTH_SHORT)
						.show();
			}
		});
	}

	// 根据数据发送异步消息，设置UI
	private void displayMessage(String value) {
		Message msg = new Message();
		if (value.contains("."))
			msg.what = 101; // 含有小数点的是密度值
		else if (value.contains("sos"))
			msg.what = 102; // 含有sos是警告
		else
			msg.what = 100; // 否则是模拟值

		msg.obj = value;
		handler.sendMessage(msg);
	}

	@Override
	protected void onDestroy() {
		if (thread != null)
			thread.interrupt();
		super.onDestroy();
	}

	@SuppressLint("HandlerLeak")
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			String string;
			switch (msg.what) {
			case 100:// 模拟值
				string = (String) msg.obj;
				txt_value.setText(string);
				break;
			case 101:// 密度值
				string = (String) msg.obj;
				txt_density.setText(string + " mg/m3");
				break;
			case 102:// 警告
				long[] pattern = { 0, 1000, 1000, 1000, 2000, 1000, 1000, 1000 };
				Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
				getSystemService(VIBRATOR_SERVICE);
				vibrator.vibrate(pattern, -1);
				txt_warning.setText("!!!");
				break;
			case 200:// 成功
				Toast.makeText(MainActivity.this, "连接设备成功", Toast.LENGTH_SHORT)
						.show();
				break;
			case 400:// 失败
				Toast.makeText(MainActivity.this, "连接设备失败", Toast.LENGTH_SHORT)
						.show();
				is_connected = false;
				break;
			}
		}
	};
}
