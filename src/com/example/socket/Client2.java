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
 * C/S�ܹ��Ŀͻ��˶��󣬳��иö��󣬿�����ʱ�����˷�����Ϣ��
 * <p>
 * ����ʱ�䣺2010-7-18 ����12:17:25
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
			System.out.println("���ض˿ڣ�" + socket.getLocalPort());
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
	 * ��ӽ��ն���Ĵ������
	 * 
	 * @param cls
	 *            ������Ķ������������ࡣ
	 * @param action
	 *            ������̶���
	 */
	public void addActionMap(Class<Object> cls, ObjectAction action) {
		actionMapping.put(cls, action);
	}

	/**
	 * �������˷��صĶ��󣬿�ʵ�ָýӿڡ�
	 */
	public static interface ObjectAction {
		void doAction(Object obj, Client2 client);
	}

	public static final class DefaultObjectAction implements ObjectAction {
		public void doAction(Object obj, Client2 client) {
			System.out.println("����\t" + obj.toString());
		}
	}

	public void sendObject(Object obj) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
		oos.writeObject(obj);
		System.out.println("���ͣ�\t" + obj);
		oos.flush();
	}

	/**
	 * ���͸��������Ϣ
	 * 
	 * @Copyright Copyright (c) 2012 - 2100
	 * @create at 2014��7��30��
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
	 * �������Է������Ϣ
	 * 
	 * @Copyright Copyright (c) 2012 - 2100
	 * @create at 2014��7��30��
	 */
	class ReceiveWatchDog implements Runnable {
		public void run() {
			while (running) {
				try {
					InputStream in = socket.getInputStream();
					if (in.available() > 0) {
						ObjectInputStream ois = new ObjectInputStream(in);
						Object obj = ois.readObject();
						System.out.println("���գ�\t" + obj);
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