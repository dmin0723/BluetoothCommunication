package android.dengmin.bluetoothcommunication.controller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.dengmin.bluetoothcommunication.connect.AcceptThread;
import android.dengmin.bluetoothcommunication.connect.ConnectThread;
import android.dengmin.bluetoothcommunication.connect.ProtocolHandler;
import android.os.Handler;

import java.io.UnsupportedEncodingException;

/**
 * Created by dmin on 2016/6/10.
 */
//ChatController.java
//聊天业务逻辑
public class ChatController {

    private ConnectThread mConnectThread;
    private AcceptThread mAcceptThread;

    /**
     * 网络协议的处理函数
     */
    private  class ChatProtocol implements ProtocolHandler<String> {

        private static final String CHARSET_NAME = "utf-8";



        //封包，以发送至网络传递
        @Override
        public byte[] encodePackage(String data) {
            if( data == null) {
                return new byte[0];
            }
            else {
                try {
                    return data.getBytes(CHARSET_NAME);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return new byte[0];
                }
            }
        }

        //解包，接收网络传递过来的数据
        @Override
        public String decodePackage(byte[] netData) {
            if( netData == null) {
                return "";
            }
            try {
                return new String(netData, CHARSET_NAME);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return "";
            }
        }
    }

    /**
     * 协议处理
     */
    private ChatProtocol mProtocol = new ChatProtocol();


    /**
     * 与服务器连接进行聊天
     * @param device
     * @param adapter
     * @param handler
     */
    public void startChatWith(BluetoothDevice device, BluetoothAdapter adapter, Handler handler) {
        mConnectThread = new ConnectThread(device,adapter,handler);
        mConnectThread.start();
    }

    /**
     * 等待客户端来连接
     * @param adapter
     * @param handler
     */
    public void waitingForFriends(BluetoothAdapter adapter, Handler handler) {
        mAcceptThread = new AcceptThread(adapter,handler);
        mAcceptThread.start();
    }


    /**
     * 发出消息
     * @param msg
     */
    public void sendMessage(String msg) {
        byte[] data = mProtocol.encodePackage(msg);
        if(mConnectThread != null) {
            mConnectThread.sendData(data);
        }
        else if( mAcceptThread != null) {
            mAcceptThread.sendData(data);
        }
    }


    /**
     * 网络数据解码
     * @param data
     * @return
     */
    public String decodeMessage(byte[] data) {
        return  mProtocol.decodePackage(data);
    }

    /**
     * 停止聊天
     */
    public void stopChat() {
        if(mConnectThread != null) {
            mConnectThread.cancel();
        }
        else if( mAcceptThread != null) {
            mAcceptThread.cancel();
        }
    }

    /**
     * 单例方式构造类对象
     */
    private static class ChatControlHolder {
        private static ChatController mInstance = new ChatController();
    }

    public static ChatController getInstance() {
        return ChatControlHolder.mInstance;
    }


}