package android.dengmin.bluetoothcommunication;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.dengmin.bluetoothcommunication.UI.Constant;
import android.dengmin.bluetoothcommunication.UI.DeviceAdapter;
import android.dengmin.bluetoothcommunication.controller.BlueToothController;
import android.dengmin.bluetoothcommunication.controller.ChatController;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //startActivityForResult()的请求码
    public static final int REQUEST_CODE = 0;
    //搜索到的蓝牙设备列表
    private List<BluetoothDevice> mDeviceList = new ArrayList<>();
    //已绑定的蓝牙设备列表
    private List<BluetoothDevice> mBondedDeviceList = new ArrayList<>();
    //管理蓝牙操作的类
    private BlueToothController mController = new BlueToothController();
    private ListView mListView;
    //自定义ListView的Adapter适配器
    private DeviceAdapter mAdapter;
    private Toast mToast;
    //聊天面板
    private View mChatPanel;
    //发送按钮
    private Button mSendBt;
    //输入框
    private EditText mInputBox;
    //显示聊天内容区域
    private TextView mChatContent;
    //输入内容
    private StringBuilder mChatText = new StringBuilder();
    //Handler，用于线程之间传递信息以更新UI
    private Handler mUIHandler = new MyHandler();

    //广播接收器，用于收听系统发出的广播以触发操作
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //接收广播附带的intent中的action
            String action = intent.getAction();
            //接收到开始搜索设备的action
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
            //在标题栏显示转动的progressBar，表示开始搜索
                setProgressBarIndeterminateVisibility(true);
                //初始化数据列表
                mDeviceList.clear();
                //刷新ListView
                mAdapter.notifyDataSetChanged();
            }
            //接收到搜索完毕的action
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //关闭标题栏转动的progressBar，表示搜索结束
                setProgressBarIndeterminateVisibility(false);
            }
            //接收到找到设备的action
            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //从extra数据中获得搜索到的蓝牙设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //找到一个，添加一个
                mDeviceList.add(device);
                //刷新ListView列表
                mAdapter.notifyDataSetChanged();
            }
            //扫描模式改变，即设备在可见性之间切换
            else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                //可见性的模式
                int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, 0);
                //本设备对其他设备可见
                if (scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    setProgressBarIndeterminateVisibility(true);
                }
                //本设备对其他设备隐藏
                else {
                    setProgressBarIndeterminateVisibility(false);
                }
            }
            //绑定状态改变
            else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                //获得绑定设备
                BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //无绑定
                if (remoteDevice == null) {
                    showToast("no device");
                    return;
                }
                int status = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0);
                //已绑定
                if (status == BluetoothDevice.BOND_BONDED) {
                    showToast("Bonded " + remoteDevice.getName());
                }
                //正在绑定
                else if (status == BluetoothDevice.BOND_BONDING) {
                    showToast("Bonding " + remoteDevice.getName());
                }
                //未绑定
                else if (status == BluetoothDevice.BOND_NONE) {
                    showToast("Not bond " + remoteDevice.getName());
                }
            }
        }
    };

    //点击搜索到的设备，并点击某一项并与之绑定时回调的接口对象
    private AdapterView.OnItemClickListener bindDeviceClick = new AdapterView.OnItemClickListener() {
        //绑定设备需要设备版本不低于Android 4.4 (API 19)
        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            //从列表中获取该设备
            BluetoothDevice device = mDeviceList.get(i);
            //绑定设备需要设备版本不低于Android 4.4 (API 19)，低于该版本的设备无法绑定蓝牙
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                //绑定设备
                device.createBond();
            }
        }
    };
    //点击已绑定设备列表中的某一项时回调的接口对象
    private AdapterView.OnItemClickListener bindedDeviceClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            //获得绑定的设备
            BluetoothDevice device = mBondedDeviceList.get(i);

            //与选中的设备聊天
            ChatController.getInstance().startChatWith(device, mController.getAdapter(), mUIHandler);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //初始化ActionBar
        initActionBar(); //报错2
        //绑定界面
        setContentView(R.layout.activity_main);
        //初始化UI控件
        initUI();

        //这是设置设备列表和对话列表的可见性
        BluetoothConfigMode();

        //注册广播接收器以接收系统广播
        registerBluetoothReceiver();
        //打开蓝牙
        mController.turnOnBlueTooth(this, REQUEST_CODE);
    }

    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter();
        //开始查找
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        //结束查找
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        //查找到设备
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        //设备扫描模式改变(可见性改变)
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        //绑定状态
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        //动态注册广播接收器
        registerReceiver(mReceiver, filter);
    }

    private void initUI() {
        mListView = (ListView) findViewById(R.id.device_list);
        mAdapter = new DeviceAdapter(mDeviceList, this);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(bindDeviceClick);
        mChatPanel = findViewById(R.id.chat_panel);
        mSendBt = (Button) findViewById(R.id.bt_send);
        mSendBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //点击发送按钮，获得输入框中的输入框
                String ext = mInputBox.getText().toString();

                //发送信息
                ChatController.getInstance().sendMessage(ext);
                //保存会话内容
                mChatText.append(ext).append("\n");
                //将输入内容显示在对话区域
                mChatContent.setText(mChatText.toString());
                //清空输入框
                mInputBox.setText("");
            }
        });
        mInputBox = (EditText) findViewById(R.id.chat_edit);
        mChatContent = (TextView) findViewById(R.id.chat_content);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ChatController.getInstance().stopChat();

        unregisterReceiver(mReceiver);
    }

    //旧
    public void enterChatMode() {
        //进入聊天界面，蓝牙列表隐藏
        mListView.setVisibility(View.GONE);
        //聊天面板出现
        mChatPanel.setVisibility(View.VISIBLE);
    }

    //旧
    public void exitChatMode() {
        //退出聊天界面，显示蓝牙列表
        mListView.setVisibility(View.VISIBLE);
        //隐藏聊天面板
        mChatPanel.setVisibility(View.GONE);
    }

    //显示对话框
    private void ChatMode() {
        mListView.setVisibility(View.GONE);
        mChatPanel.setVisibility(View.VISIBLE);

    }

    //这是显示设备列表
    private void BluetoothConfigMode() {
        mChatPanel.setVisibility(View.GONE);
        mListView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            //若用户不打算打开蓝牙功能，则activity直接被finish
            if (resultCode != RESULT_OK) {
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void showToast(String text) {

        if (mToast == null) {
            mToast = Toast.makeText(this, text, Toast.LENGTH_LONG);
        } else {
            mToast.setText(text);
        }
        mToast.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.enable_visiblity) {
            mController.enableVisibly(this);
        } else if (id == R.id.find_device) {
            //查找设备
            mAdapter.refresh(mDeviceList);
            mController.findDevice();
            mListView.setOnItemClickListener(bindDeviceClick);
            BluetoothConfigMode();
        } else if (id == R.id.bonded_device) {
            //查看已绑定设备
            mBondedDeviceList = mController.getBondedDeviceList();
            mAdapter.refresh(mBondedDeviceList);
            mListView.setOnItemClickListener(bindedDeviceClick);
            BluetoothConfigMode();
        } else if (id == R.id.listening) {
            //等待对方设备进入聊天
            ChatController.getInstance().waitingForFriends(mController.getAdapter(), mUIHandler);
        } else if (id == R.id.stop_listening) {
            ChatController.getInstance().stopChat();

        } else if (id == R.id.disconnect) {
            BluetoothConfigMode();
            mChatContent.setText("");
        }

        return super.onOptionsItemSelected(item);
    }

    private void initActionBar() {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayUseLogoEnabled(false);//报错1
        setProgressBarIndeterminate(true);
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //处理从子线层发给UI线程的消息以更新UI
    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Constant.MSG_START_LISTENING:
                    setProgressBarIndeterminateVisibility(true);
                    break;
                case Constant.MSG_FINISH_LISTENING:
                    setProgressBarIndeterminateVisibility(false);

                    break;
                case Constant.MSG_GOT_DATA:
                    byte[] data = (byte[]) msg.obj;
                    mChatText.append(ChatController.getInstance().decodeMessage(data)).append("\n");
                    mChatContent.setText(mChatText.toString());
                    break;
                case Constant.MSG_ERROR:

                    showToast("error: " + String.valueOf(msg.obj));
                    break;
                case Constant.MSG_CONNECTED_TO_SERVER:
                    showToast("Connected to Server");
                    ChatMode();
                    break;
                case Constant.MSG_GOT_A_CLINET:
                    showToast("Got a Client");
                    ChatMode();
                    break;
            }
        }
    }
}
