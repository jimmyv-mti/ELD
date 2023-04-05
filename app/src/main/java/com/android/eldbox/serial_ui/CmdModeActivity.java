package com.android.eldbox.serial_ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.eldbox.MApplication;
import com.android.eldbox.R;
import com.android.eldbox.StringUtil;
import com.android.eldbox_api.CommandEnum;
import com.hjq.toast.ToastUtils;

import java.io.IOException;

/**
 * @ProjectName: ELDBOX
 * @Package: com.android.eldbox.serial_ui
 * @ClassName: CmdModeActivity
 * @Description: Cmd模式设置界面
 * @Author: HJH
 * @CreateDate: 12/2/2019 11:15 AM
 * @UpdateUser: 更新者：HJH
 * @UpdateDate: 12/2/2019 11:15 AM
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
public class CmdModeActivity extends AppCompatActivity {

    EditText mEtReceiveCmd;
    EditText mEtSendCmd;

    Button mBtnSendCmd;

    private boolean isExit = false;

    //Intent
    public static final String sCmdDataIntent = "CmdData";
    public static final String sCmdDataIntentData = "cmdData";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cmd_mode);

        initControl();

        if(!((MApplication) getApplication()).showAsJ1939)
            mEtSendCmd.setHint("010C");

        //should change to cmd mode first
        try {
            ((MApplication) getApplication()).serialAgent.setWorkMode(CommandEnum.WorkMode.CMD_MODE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //接收数据广播 BroadcastReceiver to get data
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(sCmdDataIntent);
        registerReceiver(receiver, intentFilter);
    }

    /**
     * @method initControl
     * @description 初始化控件  Initialize control
     * @author: HJH
     * @param
     * @return * @return: void
     */
    void initControl() {
        mEtReceiveCmd = findViewById(R.id.et_receive);
        mEtSendCmd = findViewById(R.id.et_send_cmd);
        mBtnSendCmd = findViewById(R.id.btn_send_cmd);

        mBtnSendCmd.setOnClickListener(new mCmdModeClick());
    }

    /**
     * @interface
     * @description 按键监听接口  Button click implement interface
     * @author: HJH
     * @param * @Param null:
     * @return * @return: null
     */
    class mCmdModeClick implements View.OnClickListener
    {
        @Override
        public void onClick(View v) {
            switch (v.getId())
            {
                case R.id.btn_send_cmd:
                    byte[] mWant = mEtSendCmd.getText().toString().getBytes();
                    byte[] needToSend = new byte[mWant.length+1];
                    System.arraycopy(mWant,0,needToSend,0,mWant.length);
                    needToSend[mWant.length] = 0x0d;
                    try {
                        ((MApplication) getApplication()).serialAgent.writeCMDString(needToSend);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case sCmdDataIntent: {
                    mEtReceiveCmd.append(intent.getStringExtra(sCmdDataIntentData));
                    mEtReceiveCmd.append(StringUtil.sEnter);
                }
            }
        }
    };

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CmdModeActivity.this);
        builder.setTitle(R.string.exit_cmd_mode_title)
                .setMessage(R.string.exit_cmd_mode_tip)
                .setPositiveButton(R.string.exit_cmd_mode_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //change to eld mode
                        ToastUtils.show(R.string.toast_set_eld_mode);
                        dialog.cancel();
                        CmdModeActivity.this.finish();
                    }
                })
                .setNegativeButton(R.string.exit_cmd_mode_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        return;
                    }
                })
                .setCancelable(false)
                .show();

        if(!isExit)
            return;
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);
    }
}
