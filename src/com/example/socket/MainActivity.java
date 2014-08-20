package com.example.socket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.Enumeration;

import org.apache.http.conn.util.InetAddressUtils;

import com.example.socket.PServer2.ObjectAction;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements ObjectAction {

    private TextView txtvs;// ��ʾ������Ϣ
    private Button btnConnect, btnServer, btnSend;// server��ť
    private String IPAddress;
    private EditText edit_send;
    private Button btn_send;
    Client2 client;
    PServer2 server;
    boolean isServer = false;
    public Handler mHandler = new Handler() {

        @Override
        public void handleMessage(android.os.Message msg) {
            if (msg.what == 1) {
                txtvs.setText("���ӳɹ���");
            } else {
                String str = msg.obj.toString();
                if (!isDate(str)) {
                    txtvs.setText("���յ���Ϣ��" + str);
                }
            }
        };
    };

    public boolean isDate(String str) {
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            format.parse(str);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inintView();
        inintListener();
    }

    private void inintView() {
        // TODO Auto-generated method stub
        edit_send = (EditText) findViewById(R.id.edit_send);
        btn_send = (Button) findViewById(R.id.btn_send);

        btnConnect = (Button) findViewById(R.id.connect);
        btnSend = (Button) findViewById(R.id.btn_send);
        txtvs = (TextView) findViewById(R.id.textView1);
        IPAddress = getlocalip();
        if (IPAddress == null) {
            IPAddress = getLocalIpAddress();
        }
        txtvs.setText("����WIFI-IP��ַ:" + IPAddress);// ��ʼ��ʾ����IP
        btnServer = (Button) findViewById(R.id.server);
        if (IPAddress.split("\\.")[3].equals("1")) {
            openService();
        }
    }

    private void openService() {
        isServer = true;
        btnConnect.setEnabled(false);
        int port = 65432;
        server = new PServer2(MainActivity.this, port, mHandler);
        server.start();
    }

    private void inintListener() {
        /**
         * SERVER���¼�
         */
        btnServer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                openService();
            }

        });
        /**
         * client���¼�
         */
        btnConnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                btnServer.setEnabled(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            String serverIp = "192.168.1.1";
                            int port = 65432;
                            client = new Client2(mHandler, serverIp, port);
                            client.start();
                        } catch (UnknownHostException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

        });
        btn_send.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    Object objs = edit_send.getText().toString();
                    txtvs.setText("����<" + edit_send.getText().toString() + ">�ɹ�>");
                    OutputStream op = null;
                    if (isServer) {
                        if (server.socket.isConnected()) {
                            op = server.socket.getOutputStream();
                        } else {
                            Toast.makeText(MainActivity.this, "�����ѶϿ�", 2000).show();
                        }
                    } else {
                        if (client.socket.isConnected()) {
                            op = client.socket.getOutputStream();
                        } else {
                            Toast.makeText(MainActivity.this, "�����ѶϿ�", 2000).show();
                        }
                    }
                    ObjectOutputStream oop = new ObjectOutputStream(op);
                    oop.writeObject(objs);
                    System.out.println("����!!!!!!!!!!��\t" + objs);
                    oop.flush();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * ͨ��wifi���ӻ�ȡIP��ַ
     * 
     * @return
     */
    private String getlocalip() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        // Log.d(Tag, "int ip "+ipAddress);
        System.out.println(ipAddress);
        if (ipAddress == 0)
            return null;
        return ((ipAddress & 0xff) + "." + (ipAddress >> 8 & 0xff) + "." + (ipAddress >> 16 & 0xff) + "." + (ipAddress >> 24 & 0xff));
    }

    /**
     * GPRS��ַ
     */
    public String getLocalIpAddress() {
        // ----------------------
        String ipaddress = "";
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            // �������õ�����ӿ�
            while (en.hasMoreElements()) {
                NetworkInterface nif = en.nextElement();// �õ�ÿһ������ӿڰ󶨵�����ip
                Enumeration<InetAddress> inet = nif.getInetAddresses();
                // ����ÿһ���ӿڰ󶨵�����ip
                while (inet.hasMoreElements()) {
                    InetAddress ip = inet.nextElement();
                    if (!ip.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ip.getHostAddress())) {
                        ipaddress = ip.getHostAddress();
                        return ipaddress;
                    }
                }

            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ipaddress;

    }

    @Override
    public Object doAction(Activity context, Object rev) {
        // TODO Auto-generated method stub
        System.out.println("main�������أ�" + rev);
        if (rev.toString().equals("10010")) {
            Uri uri = Uri.parse("tel:10010");
            Intent it = new Intent(Intent.ACTION_CALL, uri);
            context.startActivity(it);
        }
        return rev;
    }

}