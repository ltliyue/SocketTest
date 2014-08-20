package com.example.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.TextView;

/**
 * C/S架构的服务端对象。
 * <p>
 * 创建时间：2010-7-18 上午12:17:37
 * 
 * @author HouLei
 * @since 1.0
 */
public class PServer2 {
    public Handler mHandler, mmHandler;
    private Activity context;
    private int port;
    private volatile boolean running = false;
    private long receiveTimeDelay = 3000;
    private ConcurrentHashMap<Class, ObjectAction> actionMapping = new ConcurrentHashMap<Class, ObjectAction>();
    private Thread connWatchDog;
    public Socket socket;
    boolean isLooper = false;
    boolean isConnect = false;

    public PServer2(Activity context, int port, Handler handl) {
        mmHandler = handl;
        this.context = context;
        this.port = port;

        mHandler = new Handler() {

            @Override
            public void handleMessage(android.os.Message msg) {
                if (msg.what == 1) {
                    msg.obj = msg.obj == null ? new MainActivity() : msg.obj;
                }
            };
        };
    }

    public void start() {
        if (running)
            return;
        running = true;
        connWatchDog = new Thread(new ConnWatchDog());
        connWatchDog.start();
    }

    @SuppressWarnings("deprecation")
    public void stop() {
        if (running)
            running = false;
        if (connWatchDog != null)
            connWatchDog.destroy();
    }

    public void addActionMap(Class<Object> cls, ObjectAction action) {
        actionMapping.put(cls, action);
    }

    /**
     * 要处理客户端发来的对象，并返回一个对象，可实现该接口。
     */
    public interface ObjectAction {
        Object doAction(Activity context, Object rev);
    }

    /**
     * socket服务端开启
     * 
     * @Copyright Copyright (c) 2012 - 2100
     * @create at 2014年7月30日
     */
    class ConnWatchDog implements Runnable {
        public void run() {
            try {
                ServerSocket ss = new ServerSocket(port, 5);
                while (running) {
                    socket = ss.accept();
                    new Thread(new SocketAction(socket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                PServer2.this.stop();
            }

        }
    }

    class SocketAction implements Runnable {
        Socket s;
        boolean run = true;
        long lastReceiveTime = System.currentTimeMillis();

        public SocketAction(Socket s) {
            this.s = s;
        }

        public void run() {
            while (running && run) {
                if (!isConnect) {
                    mmHandler.sendEmptyMessage(1);
                    isConnect = true;
                }
                if (System.currentTimeMillis() - lastReceiveTime > receiveTimeDelay) {
                    overThis();
                } else {
                    try {
                        InputStream in = s.getInputStream();
                        if (in.available() > 0) {
                            if (!isLooper) {
                                Looper.prepare();
                            }
                            isLooper = true;
                            ObjectInputStream ois = new ObjectInputStream(in);
                            System.out.println("ois.toString():::" + ois.toString());
                            Object obj = ois.readObject();
                            lastReceiveTime = System.currentTimeMillis();
                            System.out.println("接收：\t" + obj);
                            ObjectAction oa = actionMapping.get(obj.getClass());

                            Message message = new Message();
                            message.obj = obj.toString();
                            mmHandler.sendMessage(message);

                            oa = oa == null ? new MainActivity() : oa;
                            Object out = oa.doAction(context, obj);
                            if (out != null) {
                                ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                                oos.writeObject(out);
                                oos.flush();
                            }
                        } else {
                            Thread.sleep(10);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println(e);
                        overThis();
                    }
                }
            }
        }

        private void overThis() {
            if (run)
                run = false;
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("关闭：" + s.getRemoteSocketAddress());
        }

    }

    // public static void main(String[] args) {
    // int port = 65432;
    // PServer2 server = new PServer2(port);
    // server.start();
    // }

}