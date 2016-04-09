package com.example.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * C/S架构的客户端对象，持有该对象，可以随时向服务端发送消息。
 * <p>
 * 创建时间：2010-7-18 上午12:17:25
 * 
 * @author HouLei
 * @since 1.0
 */
public class Client2 {
	Handler mHandler;
	private String serverIp;
	private int port;
	public Socket socket;
	private boolean running = false;
	private long lastSendTime;
	private ConcurrentHashMap<Class, ObjectAction> actionMapping = new ConcurrentHashMap<Class, ObjectAction>();

	public Client2(Handler mHandler, String serverIp, int port) {
		this.mHandler = mHandler;
		this.serverIp = serverIp;
		this.port = port;
	}

	public void start() throws UnknownHostException, IOException {
		if (running)
			return;
		Looper.prepare();
		try {
			socket = new Socket(serverIp, port);
			System.out.println("本地端口：" + socket.getLocalPort());
		} catch (Exception e) {
			e.printStackTrace();
		}

		lastSendTime = System.currentTimeMillis();
		running = true;
		new Thread(new KeepAliveWatchDog()).start();
		new Thread(new ReceiveWatchDog()).start();
	}

	public void stop() {
		if (running)
			running = false;
	}

	/**
	 * 添加接收对象的处理对象。
	 * 
	 * @param cls
	 *            待处理的对象，其所属的类。
	 * @param action
	 *            处理过程对象。
	 */
	public void addActionMap(Class<Object> cls, ObjectAction action) {
		actionMapping.put(cls, action);
	}

	/**
	 * 处理服务端发回的对象，可实现该接口。
	 */
	public static interface ObjectAction {
		void doAction(Object obj, Client2 client);
	}

	public static final class DefaultObjectAction implements ObjectAction {
		public void doAction(Object obj, Client2 client) {
			System.out.println("处理：\t" + obj.toString());
		}
	}

	public void sendObject(Object obj) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
		oos.writeObject(obj);
		System.out.println("发送：\t" + obj);
		oos.flush();
	}

	/**
	 * 发送给服务端消息
	 * 
	 * @Copyright Copyright (c) 2012 - 2100
	 * @create at 2014年7月30日
	 */
	class KeepAliveWatchDog implements Runnable {
		long checkDelay = 10;
		long keepAliveDelay = 2000;

		public void run() {
			while (running) {
				if (System.currentTimeMillis() - lastSendTime > keepAliveDelay) {
					try {
						Client2.this.sendObject(new KeepAlive());
					} catch (IOException e) {
						e.printStackTrace();
						Client2.this.stop();
					}
					lastSendTime = System.currentTimeMillis();
				} else {
					try {
						Thread.sleep(checkDelay);
					} catch (InterruptedException e) {
						e.printStackTrace();
						Client2.this.stop();
					}
				}
			}
		}
	}

	/**
	 * 接收来自服务端消息
	 * 
	 * @Copyright Copyright (c) 2012 - 2100
	 * @create at 2014年7月30日
	 */
	class ReceiveWatchDog implements Runnable {
		public void run() {
			while (running) {
				try {
					InputStream in = socket.getInputStream();
					if (in.available() > 0) {
						ObjectInputStream ois = new ObjectInputStream(in);
						Object obj = ois.readObject();
						System.out.println("接收：\t" + obj);
						ObjectAction oa = actionMapping.get(obj.getClass());

						Message message = new Message();
						message.obj = obj.toString();
						mHandler.sendMessage(message);
						oa = oa == null ? new DefaultObjectAction() : oa;
						oa.doAction(obj, Client2.this);
					} else {
						Thread.sleep(10);
					}
				} catch (Exception e) {
					e.printStackTrace();
					Client2.this.stop();
				}
			}
		}
	}

	// public static void main(String[] args) throws UnknownHostException,
	// IOException {
	// String serverIp = "127.0.0.1";
	// int port = 65432;
	// Client2 client = new Client2(serverIp, port);
	// client.start();
	// }
}