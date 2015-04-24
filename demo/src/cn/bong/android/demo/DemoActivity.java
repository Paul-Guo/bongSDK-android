package cn.bong.android.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import cn.bong.android.sdk.BongConst;
import cn.bong.android.sdk.BongManager;
import cn.bong.android.sdk.config.Environment;
import cn.bong.android.sdk.event.*;
import cn.bong.android.sdk.model.ble.ConnectState;
import cn.bong.android.sdk.model.ble.ConnectUiListener;
import cn.bong.android.sdk.model.bong.BongType;
import cn.bong.android.sdk.model.http.auth.AuthError;
import cn.bong.android.sdk.model.http.auth.AuthInfo;
import cn.bong.android.sdk.model.http.auth.AuthUiListener;
import cn.bong.android.sdk.model.http.data.DataSyncError;
import cn.bong.android.sdk.model.http.data.DataSyncState;
import cn.bong.android.sdk.model.http.data.DataSyncUiListener;
import cn.bong.android.sdk.model.http.data.ErrorType;
import cn.bong.android.sdk.utils.DialogUtil;
import com.litesuits.android.log.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class DemoActivity extends Activity implements View.OnClickListener {

    private static final String TAG = DemoActivity.class.getSimpleName();
    private static final int MAX_WAIT = 6;// 10 有效控制时间
    private ListView listView;
    private ArrayList<BongEvent> events = new ArrayList<BongEvent>();
    private EventAdapter adapter = new EventAdapter();
    private int seconds = MAX_WAIT;
    private LinearLayout hideView;
    private Button vibrate;
    private Button light;
    private Button startSensor;
    private Button stopSensor;
    //private Button       userInfo;
    private Button userAuth;
    private Button clear;
    private Button btStartScann;
    private Button btSyncData;
    private Button btRssi;
    //private Button       btGetBongMac;
    private TextView timeTips;
    private Activity activity = this;

    private ProgressDialog progressDialog;
    private ProgressDialog syncProgressDialog;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        findViews();
        hideView.setVisibility(View.GONE);
        listView.setAdapter(adapter);
        progressDialog = new ProgressDialog(activity);
        syncProgressDialog = new ProgressDialog(activity);

        // 测试用AppID（仅测试环境）
        //client_id  1419735044202
        //sign_key 7ae31974a95fec07ad3d047c075b11745d8ce989
        //client_secret  558860f5ba4546ddb31eafeee11dc8f4

        // 初始化sdk
        //BongManager.initialize(this, "1419735044202", "", "558860f5ba4546ddb31eafeee11dc8f4");
        BongManager.initialize(this, "1415266387250", "", "7d9b930cabff430a96adce868f90fc85");
        // 开启 调试模式，打印日志
        BongManager.setDebuged(true);
        // 设置 测试环境
        BongManager.setEnvironment(Environment.Daily);

        refreshButton();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    seconds--;
                    if (seconds > 0) {
                        timeTips.setText(seconds + ": 触摸后可以向bong发送指令。");
                        handler.sendEmptyMessageDelayed(0, 1000);
                    } else {
                        hideView.setVisibility(View.GONE);
                    }
                    break;
                case 1:
                    DataEvent e = (DataEvent) msg.obj;
                    progressDialog.setTitle("按back键停止");
                    progressDialog.setMessage("x: " + e.getX() + ", y: " + e.getY() + ", z: " + e.getZ());
            }
        }
    };

    @Override
    public void onClick(View v) {
        // 以下操作全部在触摸之后10秒内执行
        if (v == vibrate) {
            //  震动示例
            BongManager.bongVibrate(3, null);
        } else if (v == light) {
            // 亮灯示例
            BongManager.bongLight(3, null);
        } else if (v == startSensor) {
            // 开启传感器示例
            if (!sensorUiListener.isInConntecting()) {
                BongManager.bongStartSensorOutput(sensorUiListener);
            }
        } else if (v == stopSensor) {
            //  关闭传感器示例
            BongManager.bongStopSensorOutput(sensorUiListener);
        } else if (v == clear) {
            // 清除事件日志 
            events.clear();
            adapter.notifyDataSetChanged();
            //
        } else if (v == userAuth) {
            if (BongManager.isSessionValid()) {
                // 取消授权
                BongManager.bongClearAuth();
                refreshButton();
            } else {
                // 开始授权
                BongManager.bongAuth(this, "demo", new AuthUiListener() {
                    @Override
                    public void onError(AuthError error) {
                        DialogUtil.showTips(
                                activity,
                                "授权失败", " code  : " + error.code
                                        + "\nmsg   : " + error.message
                                        + "\ndetail: " + error.errorDetail);
                    }

                    @Override
                    public void onSucess(AuthInfo result) {
                        DialogUtil.showTips(
                                activity,
                                "授权成功", " state : " + result.state
                                        + "\ntoken : " + result.accessToken
                                        + "\nexpire: " + result.expiresIn
                                        + "\nuid   : " + result.uid
                                        + "\nscope : " + result.scope
                                        + "\nrefreh_expire: " + result.refreshTokenExpiration
                                        + "\nrefreh_token : " + result.refreshToken
                                        + "\ntokenType    : " + result.tokenType);
                        refreshButton();
                    }

                    @Override
                    public void onCancel() {
                        //DialogUtil.showTips(activity, "提示", "授权取消");
                    }
                });

            }

        } else if (v == btStartScann) {
            if (BongManager.isTouchCatching()) {
                // 关闭
                BongManager.turnOffTouchEventListen(this);
                btStartScann.setText("开始触摸监听");
            } else {
                BongManager.turnOnTouchEventListen(this, new TouchEventListener() {
                    @Override
                    public void onTouch(TouchEvent event) {
                        if (BongManager.getBongType() == BongType.bong2) {
                            events.add(event);
                            adapter.notifyDataSetChanged();
                            showMoreActions();
                        } else {
                            if (event.getTouchType() == TouchType.None) {
                                setTitle(R.string.app_name);
                            } else {
                                setTitle(event.getTouchType().toString());
                                events.add(event);
                                adapter.notifyDataSetChanged();
                            }
                            // bongX 或者 bongXX 的具体事件类型如下
                            switch (event.getTouchType()) {
                                case None:
                                    setTitle(R.string.app_name);
                                    break;
                                case ToLeft:
                                    // 向左滑动
                                    break;
                                case ToRight:
                                    // 向右滑动
                                    break;
                                case ToTop:
                                    // 向上滑动
                                    break;
                                case ToBottom:
                                    // 向下滑动
                                    break;
                                case Clockwise:
                                    //顺时针滑动
                                    break;
                                case AntiClockwise:
                                    //逆时针滑动
                                    break;
                                case SleepIn:
                                    //睡眠进入
                                    break;
                                case SleepOut:
                                    //睡眠退出
                                    break;
                                case BongIn:
                                    //进入bong状态
                                    break;
                                case BongOut:
                                    //退出bong状态
                                    break;
                                default:
                                    break;
                            }
                        }
                    }

                    @Override
                    public void onLongTouch(TouchEvent event) {
                        events.add(event);
                        adapter.notifyDataSetChanged();
                        if (!BongManager.getBongType().isBongXorXX()) {
                            showMoreActions();
                        }
                    }
                });
                btStartScann.setText("关闭触摸监听");
            }
        } else if (v == btSyncData) {
            if (BongManager.isDataSyncing()) {
                DialogUtil.showTips(activity, null, "正在同步...");
            } else {
                AlertDialog.Builder builder = DialogUtil.dialogBuilder(this, "选择同步方式", null);
                builder.setItems(new String[]{"增量同步：最后一次同步到现在",
                                              "同步过去的48小时到现在", "同步指定时间内数据"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                BongManager.bongDataSyncnizedByUpdate(listener);
                                break;
                            case 1:
                                BongManager.bongDataSyncnizedByHours(listener, System.currentTimeMillis(), 48);
                                break;
                            case 2:
                                // 过去N个小时数据
                                int hour = 60 * 60000;
                                int N = 4;
                                long endTime = System.currentTimeMillis();
                                long startTime = endTime - hour * N;

                                BongManager.bongDataSyncnizedByTime(listener, startTime, endTime);
                                break;
                        }
                        refreshButton();
                    }
                });
                builder.setPositiveButton("取消", null);
                builder.show();
            }
        } else if (v == btRssi) {
            if (!BongManager.getBongType().isBongXorXX()) {
                DialogUtil.showTips(activity, null, "仅bongX 和bongXX 支持获取距离");
            }
            if (!BongManager.isRssiGeting()) {
                BongManager.turnOnRssitListen(this, new RssiListener() {
                    @Override
                    public void onRssi(int rssi) {
                        if (BongManager.isRssiGeting() && !progressDialog.isShowing()) {
                            progressDialog.setTitle("连接中");
                            progressDialog.setMessage("请触摸 Yes! 键...");
                            progressDialog.setCancelable(true);
                            progressDialog.setCanceledOnTouchOutside(false);
                            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    BongManager.turnOffRssitListen(DemoActivity.this);
                                    progressDialog.dismiss();
                                    refreshButton();
                                }
                            });
                            progressDialog.show();
                        } else if (progressDialog.isShowing()) {
                            progressDialog.setTitle("按back键停止");
                            progressDialog.setMessage("rssi: " + rssi);
                        }
                    }
                });
            } else {
                BongManager.turnOffRssitListen(this);
                progressDialog.dismiss();
            }
            refreshButton();

        }
    }

    ConnectUiListener sensorUiListener = new ConnectUiListener() {


        @Override
        public void onStateChanged(ConnectState state) {
            if (state == ConnectState.Scanning) {
                progressDialog.setTitle("连接中");
                progressDialog.setMessage("请触摸 Yes! 键...");
                progressDialog.setCancelable(true);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        BongManager.bongStopSensorOutput(sensorUiListener);
                    }
                });
            } else if (state == ConnectState.Connecting) {
                progressDialog.setMessage("连接中...");
            }
        }

        @Override
        public void onFailed(String msg) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            DialogUtil.showTips(activity, "读取失败", msg);
        }

        @Override
        public void onSucess() {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }

        @Override
        public void onDataReadInBackground(byte[] data) {
            Log.v(TAG, "data: " + Arrays.toString(data));
            if (data.length > 5) {
                DataEvent e = new DataEvent(System.currentTimeMillis(), data[1], data[3], data[5]);
                Message msg = handler.obtainMessage(1);
                msg.obj = e;
                msg.sendToTarget();
            }

        }
    };

    DataSyncUiListener listener = new DataSyncUiListener() {

        @Override
        public void onStateChanged(DataSyncState state) {
            if (state == DataSyncState.Scanning) {
                syncProgressDialog.setTitle("同步中");
                if (BongManager.getBongType().isBongXorXX()) {
                    syncProgressDialog.setMessage("正在扫描设备...");
                } else {
                    syncProgressDialog.setMessage("请触摸 Yes! 键...");
                }
                syncProgressDialog.show();
            } else if (state == DataSyncState.Connecting) {
                syncProgressDialog.setMessage("发现设备，正在同步...");
            } else if (state == DataSyncState.Uploading) {
                syncProgressDialog.setMessage("同步完成，正在上传...");
            }
        }

        @Override
        public void onError(DataSyncError error) {
            if (syncProgressDialog.isShowing()) {
                syncProgressDialog.dismiss();
            }
            DialogUtil.showTips(activity, error.message, error.errorDetail);
            refreshButton();

            if (error.getCode() == ErrorType.ERROR_CODE_UNBIND) {
                // 异步请求获取最新绑定的手环mac。
                BongManager.bongRefreshMacAsync(null);
            }
        }

        @Override
        public void onSucess() {
            if (syncProgressDialog.isShowing()) {
                syncProgressDialog.dismiss();
            }
            DialogUtil.showTips(activity, "同步成功", "数据已上传至云端");
            refreshButton();
        }
    };


    private void showMoreActions() {
        hideView.setVisibility(View.VISIBLE);
        seconds = MAX_WAIT;
        handler.removeMessages(0);
        handler.sendEmptyMessageDelayed(0, 1000);
    }

    private void findViews() {
        hideView = (LinearLayout) findViewById(R.id.llHideView);
        vibrate = (Button) findViewById(R.id.btVibrate);
        light = (Button) findViewById(R.id.btLight);
        startSensor = (Button) findViewById(R.id.btStartSensor);
        stopSensor = (Button) findViewById(R.id.btStopSensor);
        //userInfo = (Button) findViewById(R.id.btUserInfo);
        userAuth = (Button) findViewById(R.id.btUserAuth);
        clear = (Button) findViewById(R.id.btClear);
        btSyncData = (Button) findViewById(R.id.btSyncData);
        btRssi = (Button) findViewById(R.id.btRssi);
        btStartScann = (Button) findViewById(R.id.btStartScann);
        listView = (ListView) findViewById(R.id.listView);
        timeTips = (TextView) findViewById(R.id.tvTimeTips);

        vibrate.setOnClickListener(this);
        light.setOnClickListener(this);
        startSensor.setOnClickListener(this);
        stopSensor.setOnClickListener(this);
        //userInfo.setOnClickListener(this);
        userAuth.setOnClickListener(this);
        clear.setOnClickListener(this);
        btSyncData.setOnClickListener(this);
        btRssi.setOnClickListener(this);
        btStartScann.setOnClickListener(this);

    }

    private void refreshButton() {
        if (BongManager.isTouchCatching()) {
            btStartScann.setText("关闭触摸监听");
        } else {
            btStartScann.setText("开始触摸监听");
        }
        if (BongManager.isDataSyncing()) {
            btSyncData.setText("正在同步...");
        } else {
            btSyncData.setText("开始同步");
        }
        if (BongManager.isRssiGeting()) {
            btRssi.setText("停止获取");
        } else {
            btRssi.setText("获取距离");
        }
        if (BongManager.isSessionValid()) {
            userAuth.setText("取消授权");
        } else {
            userAuth.setText("开始授权");
        }
    }

    public void showTips(String tip) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setMessage(tip);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    class EventAdapter extends BaseAdapter {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss:SSS");

        @Override
        public int getCount() {
            return events.size();
        }

        @Override
        public BongEvent getItem(int position) {
            return events.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_event, null);
            }
            TextView tv = (TextView) convertView;
            if (tv != null && events != null) {
                BongEvent event = getItem(position);
                switch (event.getEventType()) {
                    case BongConst.EVENT_YES_TOUCH:
                        // 短触：触摸 yes! 键 1秒左右
                        TouchEvent touchEvent = (TouchEvent) event;
                        if (BongManager.getBongType() == BongType.bong2) {
                            tv.setText(String.format("%-6s", (position + 1) + ".") + "短触 Yes! 键  "
                                       + format.format(new Date(touchEvent.getTime())));
                        } else {
                            tv.setText(String.format("%-6s", (position + 1) + ".") + touchEvent.getEventType()
                                       + format.format(new Date(touchEvent.getTime())));
                        }
                        break;
                    case BongConst.EVENT_YES_LONG_TOUCH:
                        // 长触：触摸 yes! 键 3秒左右
                        tv.setText(String.format("%-6s", (position + 1) + ".") + "长触 Yes! 键  "
                                   + format.format(new Date(event.getTime())));
                        break;
                    case BongConst.EVENT_DATA_XYZ:
                        // 数据：接收传感器 xyz 三轴原始数据：200秒连接时间，超时自动断开。
                        DataEvent dataEvent = (DataEvent) event;
                        tv.setText(String.format("%-6s", (position + 1) + ".") + "数据传输 X：" + String.format("%-4s",
                                                                                                           dataEvent
                                                                                                                   .getX())
                                   + "  Y: " + String.format("%-4s", dataEvent.getY())
                                   + "  Z: " + String.format("%-4s", dataEvent.getZ())
                                   + "  " + format.format(new Date(event.getTime())));
                        showMoreActions();
                        break;
                    default:
                        break;
                }
            }
            return convertView;
        }
    }
}
