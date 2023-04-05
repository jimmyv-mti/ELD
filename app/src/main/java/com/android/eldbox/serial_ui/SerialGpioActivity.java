package com.android.eldbox.serial_ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.eldbox.MApplication;
import com.android.eldbox.R;
import com.android.eldbox_api.Response;
import com.hjq.toast.ToastUtils;

import java.io.IOException;

/**
 * @ProjectName: ELDBOX
 * @Package: com.android.eldbox.serial_ui
 * @ClassName: SerialGpioActivity
 * @Description: GPIO和ADC读取和设置
 * @Author: HJH
 * @CreateDate: 5/7/2020 10:07 AM
 * @UpdateUser: 更新者：
 * @UpdateDate: 5/7/2020 10:07 AM
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
public class SerialGpioActivity extends AppCompatActivity {
    //Debug
    private static final String TAG = "SerialGpioActivity";

    //Intent
    public static final String sGpioDataIntent = "GpioResponse";
    public static final String sGpioDataIntentType = "gpioResponseType";
    public static final String sGpioDataIntentData = "gpioResponseData";

    private int includeResource[] = { R.id.item_gpio1, R.id.item_gpio2, R.id.item_gpio3, R.id.item_gpio4,
            R.id.item_gpio5, R.id.item_gpio6,R.id.item_gpio7, R.id.item_gpio8,
            R.id.item_gpio9, R.id.item_gpio10,R.id.item_gpio11, R.id.item_gpio12,
            R.id.item_gpio13, R.id.item_gpio14,R.id.item_gpio15, R.id.item_gpio16,
    };

    private TextView tv_gpio_title[] = new TextView[16];
    private RadioButton  rbtn_gpio_high[]   = new RadioButton[16];
    private RadioButton  rbtn_gpio_low[]  = new RadioButton[16];

    Button btn_get_gpio_status,btn_set_power_out_enable,btn_set_power_out_disable,btn_get_adc_value;

    Spinner spin_channel;

    EditText et_vref,et_value;

    private int spin_pos = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpio_and_adc);

        initControl();

        //接收数据广播 BroadcastReceiver to get data
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(sGpioDataIntent);
        registerReceiver(receiver, intentFilter);
    }

    /**
     * @param
     * @return * @return: void
     * @method initControl
     * @description 初始化控件  Initialize control
     * @author: HJH
     */
    void initControl() {
        Resources res = getResources();
        String[] gpioName = res.getStringArray(R.array.gpio_group);

        for(int i= 0;i<16;i++)
        {
            tv_gpio_title[i] = findViewById(includeResource[i]).findViewById(R.id.tv_gpio_title);
            rbtn_gpio_high[i] = findViewById(includeResource[i]).findViewById(R.id.rbtn_high);
            rbtn_gpio_low[i] = findViewById(includeResource[i]).findViewById(R.id.rbtn_low);
            //set title text
            tv_gpio_title[i].setText(gpioName[i]);
            //set Tag
            rbtn_gpio_high[i].setTag(i);
            rbtn_gpio_low[i].setTag(16+i);
            rbtn_gpio_low[i].setChecked(true);
            //set onClick
            rbtn_gpio_high[i].setOnClickListener(new mGpioRadioButtonClick());
            rbtn_gpio_low[i].setOnClickListener(new mGpioRadioButtonClick());
        }

        btn_get_gpio_status = findViewById(R.id.btn_get_gpio_status);
        btn_set_power_out_enable =findViewById(R.id.btn_set_power_output_enable);
        btn_set_power_out_disable = findViewById(R.id.btn_set_power_output_disable);
        btn_get_adc_value = findViewById(R.id.btn_get_adc_value);

        spin_channel = findViewById(R.id.spin_channel);
        et_vref = findViewById(R.id.et_reference_voltage);
        et_value = findViewById(R.id.et_adc_value);

        btn_get_gpio_status.setOnClickListener(new mGpioRadioButtonClick());
        btn_set_power_out_enable.setOnClickListener(new mGpioRadioButtonClick());
        btn_set_power_out_disable.setOnClickListener(new mGpioRadioButtonClick());
        btn_get_adc_value.setOnClickListener(new mGpioRadioButtonClick());

        spin_channel.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            //选择item的选择点击监听事件
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                spin_pos = i;
            }
            public void onNothingSelected(AdapterView<?> arg0) {
                spin_pos = 0;
            }
        });

    }

    /**
     * @param * @Param null:
     * @interface
     * @description 按键监听接口  Button click implement interface
     * @author: HJH
     * @return * @return: null
     */
    class mGpioRadioButtonClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.rbtn_high:
                    setGpioValue(1,(int)v.getTag());
                    break;
                case R.id.rbtn_low:
                    setGpioValue(0,(int)v.getTag()-16);
                    break;
                case R.id.btn_get_gpio_status:
                    try {
                        if (((MApplication) getApplication()).serialAgent != null)
                            ((MApplication)getApplication()).serialAgent.requestGpioVal(new byte[]{(byte)0x00 , (byte)0x3f});
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case R.id.btn_set_power_output_enable:
                    btn_set_power_out_enable.setEnabled(false);
                    btn_set_power_out_disable.setEnabled(true);
                    try {
                        if (((MApplication) getApplication()).serialAgent != null)
                            ((MApplication)getApplication()).serialAgent.setPwrOut(true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case R.id.btn_set_power_output_disable:
                    btn_set_power_out_enable.setEnabled(true);
                    btn_set_power_out_disable.setEnabled(false);
                    try {
                        if (((MApplication) getApplication()).serialAgent != null)
                            ((MApplication)getApplication()).serialAgent.setPwrOut(false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case R.id.btn_get_adc_value:
                    try {
                        if (((MApplication) getApplication()).serialAgent != null)
                            ((MApplication)getApplication()).serialAgent.requestAdcVal(spin_pos);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * @method setGpioValue
     * @description 设置GPIO为高或低 set Gpio output high or low
     * @date: 5/8/2020 5:14 PM
     * @author: HJH
     * @param * @Param gpioValue: 1 is high,0 is low
     * @Param gpioPos:range 0~15,for gpio1~gpio16
     * @return * @return: void
     */
    private void setGpioValue(int gpioValue,int gpioPos){
        try {
            if (((MApplication) getApplication()).serialAgent != null) {
                if (gpioValue == 1)
                    ((MApplication) getApplication()).serialAgent.setGpioOutHigh(gpioSetCommandArray(gpioPos));
                else if (gpioValue == 0) {
                    ((MApplication) getApplication()).serialAgent.setGpioOutLow(gpioSetCommandArray(gpioPos));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @method setGpioCommandArray
     * @description fetch byte array for set gpio high or low
     * @date: 5/8/2020 5:07 PM
     * @author: HJH
     * @Param gpioPos: range 0~15,for gpio1~gpio16
     * @return * @return: void
     */
    public byte[] gpioSetCommandArray(int gpioPos) {
        byte[] gpioSetArray = new byte[]{(byte)0x00, (byte)0x00};
        if(gpioPos > 7 && gpioPos < 16){
            gpioPos -= 8;
            gpioSetArray[0] |= 1 << gpioPos;// 将nPos的bit位设置为1，其他位不变
        }
        else if(gpioPos >= 0 && gpioPos < 8){
            gpioSetArray[1] |= 1 << gpioPos;// 将nPos的bit位设置为1，其他位不变
        }
        return gpioSetArray;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case sGpioDataIntent: {
                    int responseType = intent.getIntExtra(sGpioDataIntentType,0);
                    byte[] responseData = intent.getByteArrayExtra(sGpioDataIntentData);

                    //Log.v(TAG,"responseType " + responseType + " responseData " + bytes2hex(responseData));

                    if(responseData == null)
                        break;
                    int reslut = responseData[0] & 0xff;
                    switch (responseType) {
                        case Response.SetGpioOutHighSetResultResponse:
                            if(reslut == 0) {
                                //0x00 设置GPIO输出高失败 Failed to set gpio output high
                                ToastUtils.show(R.string.toast_set_gpio_high_failed);
                            }else {
                                //0xFF 设置GPIO输出高成功 Set gpio output high successfully
                                ToastUtils.show(R.string.toast_set_gpio_high_success);
                            }
                            break;
                        case Response.SetGpioOutLowSetResultResponse:
                            if(reslut == 0) {
                                //0x00 设置GPIO输出低失败 Failed to set gpio output low
                                ToastUtils.show(R.string.toast_set_gpio_low_failed);
                            }else {
                                //0xFF 设置GPIO输出低成功 Set gpio output low successfully
                                ToastUtils.show(R.string.toast_set_gpio_low_success);
                            }
                            break;
                        case Response.GetGpioValResponse:
                            freshGpioVal(responseData);
                            break;
                        case Response.GetAdcValResponse:
                            int channel = responseData[0] & 0xff;
                            int data = ((responseData[1] << 8) & 0xff00)  + (responseData[2] & 0xff);
                            float vref = et_vref.getText().toString().equals("")?3.36f:Float.parseFloat(et_vref.getText().toString());
                            float value = 0.0f;
                            if(channel == 3) {
                                if(vref == 3.36f)
                                    value = ((MApplication) getApplication()).serialAgent.computeAdcVal_for_channel3(data);
                                else if(vref > 0)
                                    value = ((MApplication) getApplication()).serialAgent.computeAdcVal(data,vref,0.5f);
                                else
                                    ToastUtils.show(R.string.toast_reference_voltage_more_than_zero);
                            }
                            else {
                                if(vref == 3.36f)
                                    value = ((MApplication) getApplication()).serialAgent.computeAdcVal(data);
                                else if(vref > 0)
                                    value = ((MApplication) getApplication()).serialAgent.computeAdcVal(data,vref,0.0f);
                                else
                                    ToastUtils.show(R.string.toast_reference_voltage_more_than_zero);
                            }
                            float showValue = (float)(Math.round(value * 10000))/10000;//保留4位小数，四舍五入 Retain 4 decimal places, rounded
                            et_value.setText(showValue + "");
                            break;
                        case Response.SetPwrOutSetResultResponse:
                            if(reslut == 0) {
                                //0x00 设置Power输出失败 Failed to set power output
                                ToastUtils.show(R.string.toast_set_power_output_failed);
                            }else {
                                //0xFF 设置Power输出成功 Set power output successfully
                                ToastUtils.show(R.string.toast_set_power_output_success);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    };

    /**
     * @method freshGpioVal
     * @description update radioButton state
     * @date: 5/11/2020 3:27 PM
     * @author: HJH
     * @param * @Param mask_and_value:
     * @return * @return: void
     */
    public void freshGpioVal(byte[] mask_and_value){
        for(int i =0; i < 8; i++){
            if((mask_and_value[0]>>i & 0x01) == 1){
                //Log.v(TAG,"freshGpioVal gpio 9~16 which " + i);
                int value = mask_and_value[2]>>i & 0x01;
                if(value == 1)
                    rbtn_gpio_high[i+8].setChecked(true);
                else if (value == 0)
                    rbtn_gpio_low[i+8].setChecked(true);
            }

            if((mask_and_value[1]>>i & 0x01) == 1){
                //Log.v(TAG,"freshGpioVal gpio 1~8 which " + i);
                int value = mask_and_value[3]>>i & 0x01;
                if(value == 1)
                    rbtn_gpio_high[i].setChecked(true);
                else if (value == 0)
                    rbtn_gpio_low[i].setChecked(true);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);

        ToastUtils.cancel();
    }

}
