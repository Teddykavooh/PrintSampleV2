package com.printsample.v2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.BarcodeFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import vpos.apipackage.PosApiHelper;
import vpos.apipackage.PrintInitException;

public class MainActivity extends AppCompatActivity {

    PosApiHelper posApiHelper = PosApiHelper.getInstance();
    final int PRINT_TEXT = 0;
    final int PRINT_OPEN = 1;
    SharedPreferences sp;
    private final static int ENABLE_RG = 10;
    private final static int DISABLE_RG = 11;
    int IsWorking = 0;
    int ret = -1;
    private boolean m_bThreadFinished = true;
    public int RESULT_CODE = 0;
    private int voltage_level;
    private int BatteryV;
    private BroadcastReceiver receiver;
    public String tag = "MainActivity";
    private String text;
    public static String[] MY_PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MOUNT_UNMOUNT_FILESYSTEMS"};

    public static final int REQUEST_EXTERNAL_STORAGE = 1;
    private int printerStatus;
    private Button offButton;
    private Button onButton;
    int checkCallPhonePermission;
    final static String TARGET_BASE_PATH = Environment.getExternalStorageDirectory() + "/";
    String deviceId;
    private int my_ret;
    private String enteredText;
    @Override
    public void onStart() {
        super.onStart();
    }

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Whitelist app
        AppWhiteListConfig();

        setContentView(R.layout.activity_main);
        onButton = findViewById(R.id.printOn);
        offButton = findViewById(R.id.printOff);
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.e("onCreate_device_id", deviceId);

        //Determine if the current Android version is >=23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermission();
        } else {
            Toast.makeText(getApplicationContext(), "Android version is below requirement.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.reConfig) {
            if (checkCallPhonePermission == PackageManager.PERMISSION_GRANTED) {
                /*Functionality*/
                Context context = getApplicationContext();
                String path = Environment.getExternalStorageDirectory() + "/BBFontUnicode_1.bin";
                String path2 = Environment.getExternalStorageDirectory() + "/MCU_C_App.bin";
                File file = new File(path);
                File file2 = new File(path2);
                if (!file.exists() && !file2.exists()) {
                    if (deviceId.equals("")) {
                        /*Default:e7171c1fe9945676*/
                        copyFilesToSdCard();
                    } else {
                        Toast.makeText(getApplicationContext(), "You are not verified for this service.", Toast.LENGTH_SHORT).show();
                        new AlertDialog.Builder(MainActivity.this)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle("VERIFICATION")
                                .setMessage("In need of activation?\n" + "Call +254721555001, " +
                                        "+254797847747\n" + "or\n" + "email: androidposkenya.co.ke"
                                        + "\n" + "Your ID: " + deviceId)
                                .setNegativeButton("BACK", null)
                                .show();
                    }
                } else {
                    Toast.makeText(context, "No need to reconfigure.", Toast.LENGTH_SHORT).show();
                }
            } else {
                requestPermission();
            }
            return true;
        }
        else if (item.getItemId() == R.id.exit) {
            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_baseline_exit_to_app_24)
                    .setTitle("EXIT")
                    .setMessage("Quit all application processes?")
                    .setPositiveButton("EXIT", (dialogInterface, i) -> {
                        onDestroy();
                        finishAffinity();
                        System.exit(0);
                    })
                    .setNegativeButton("BACK", null)
                    .show();
            return true;
        }
        else if (item.getItemId() == R.id.openPrint) {
            onClickPrintOpen();
            return true;
        }
        else if (item.getItemId() == R.id.setGray) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            // Set the title and message for the dialog
            builder.setTitle("Set Gray mode:");
            builder.setMessage("Please enter your mode(1~5):");

            // Create an EditText view to get user input
            final EditText gr_editText = new EditText(this);
            gr_editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            gr_editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)}); // Restrict input to 1 digits
            builder.setView(gr_editText);

            // Set the positive button and its click listener
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Get the entered text from the EditText
                    enteredText = gr_editText.getText().toString();

                    // Do something with the entered text
                    // For example, display a Toast message
                    Toast.makeText(getApplicationContext(), "Entered Gray Mode: " + enteredText, Toast.LENGTH_SHORT).show();
                }
            });

            // Set the negative button and its click listener
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Cancel button clicked, do something or simply dismiss the dialog
                    dialog.dismiss();
                }
            });

            // Create and show the AlertDialog
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*Paste file to Internal storage*/
    private void copyFilesToSdCard() {
        copyFileOrDir(""); // copy all files in assets folder in my project
    }

    private void copyFileOrDir(String path) {
        AssetManager assetManager = this.getAssets();
        String[] assets;
        try {
            Log.i("tag", "copyFileOrDir() " + path);
            assets = assetManager.list(path);
            if (assets.length == 0) {
                copyFile(path);
            } else {
                String fullPath =  TARGET_BASE_PATH + path;
                Log.i("tag", "path="+fullPath);
                File dir = new File(fullPath);
                if (!dir.exists() && !path.startsWith("images") && !path.startsWith("sounds") && !path.startsWith("webkit"))
                    if (!dir.mkdirs())
                        Log.i("tag", "could not create dir "+fullPath);
                for (String asset : assets) {
                    String p;
                    if (path.equals(""))
                        p = "";
                    else
                        p = path + "/";

                    if (!path.startsWith("images") && !path.startsWith("sounds") && !path.startsWith("webkit"))
                        copyFileOrDir(p + asset);
                }
            }
        } catch (IOException ex) {
            Log.e("tag", "I/O Exception", ex);
            Toast.makeText(this, "Reconfiguration failed!", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyFile(String filename) {
        AssetManager assetManager = this.getAssets();

        InputStream in;
        OutputStream out;
        String newFileName = null;
        try {
            Log.i("tag", "copyFile() "+filename);
            in = assetManager.open(filename);
            if (filename.endsWith(".jpg")) // extension was added to avoid compression on APK file
                newFileName = TARGET_BASE_PATH + filename.substring(0, filename.length()-4);
            else
                newFileName = TARGET_BASE_PATH + filename;
            out = new FileOutputStream(newFileName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) {
            Log.e("tag", "Exception in copyFile() of "+newFileName);
            Log.e("tag", "Exception in copyFile() "+ e);
        }

    }


    /**
     * @Description: Request permission
     **/
    private void requestPermission() {
        //Check if there is write permission
        checkCallPhonePermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
            //Without the permission to Write, to apply for the permission to Read and Write, the system will pop up the permission dialog
            ActivityCompat.requestPermissions(MainActivity.this, MY_PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        } else {
            Toast.makeText(getApplicationContext(), "Permissions granted already!!!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * a callback for request permission
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Permissions already granted!!!",
                        Toast.LENGTH_SHORT).show();
            } else {
//                Toast.makeText(MainActivity.this,R.string.title_permission,Toast.LENGTH_SHORT).show();
                requestPermission();
            }
        }

    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub

        disableFunctionLaunch(true);
        /*getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);*/

        super.onResume();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        receiver = new BatteryReceiver();
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        disableFunctionLaunch(false);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PosApiHelper.getInstance().SysLogSwitch((byte)0);
        enableFunctionLaunch(true);
    }

    /*Turning printer on*/
    public void turnOn(View v){
        printerStatus = 1;
        posApiHelper.SysLogSwitch((byte)printerStatus);
        onButton.setVisibility(View.INVISIBLE);
        offButton.setVisibility(View.VISIBLE);
        Toast.makeText(getApplicationContext(), "Printer turned ON", Toast.LENGTH_SHORT).show();
    }



    /*Turning printer off*/
    public void turnOff(View v){
        printerStatus = 0;
        posApiHelper.SysLogSwitch((byte)0);
        offButton.setVisibility(View.INVISIBLE);
        onButton.setVisibility(View.VISIBLE);
        Toast.makeText(getApplicationContext(), "Printer turned OFF", Toast.LENGTH_SHORT).show();
    }

    /*disable the power key to avoid print process interruption. Recommended to block the return,
    power, Home buttons as well*/
    private static final String DISABLE_FUNCTION_LAUNCH_ACTION = "android.intent.action.DISABLE_FUNCTION_LAUNCH";
    private void disableFunctionLaunch(boolean state) {
        Intent disablePowerKeyIntent = new Intent(DISABLE_FUNCTION_LAUNCH_ACTION);
        if (state) {
            disablePowerKeyIntent.putExtra("state", true);
        } else {
            disablePowerKeyIntent.putExtra("state", false);
        }
        sendBroadcast(disablePowerKeyIntent);
    }

    //Enable keys back
    private static final String ENABLE_FUNCTION_LAUNCH_ACTION = "android.intent.action.ENABLE_FUNCTION_LAUNCH";
    private void enableFunctionLaunch(boolean state) {
        Intent enablePowerKeyIntent = new Intent(ENABLE_FUNCTION_LAUNCH_ACTION);
        if (state) {
            enablePowerKeyIntent.putExtra("state", true);
        } else {
            enablePowerKeyIntent.putExtra("state", false);
        }
        sendBroadcast(enablePowerKeyIntent);
    }


    /*Printing Activity begins*/
    public void onClickPrint(View v) {
        EditText myText = findViewById(R.id.editText);
        text = myText.getText().toString();
        if (printerStatus == 0) {
            Toast.makeText(getApplicationContext(), "Turn printer ON to continue!!!",
                    Toast.LENGTH_SHORT).show();
        } else {
            if (printThread != null && printThread.isThreadFinished()) {
                Log.e(tag, "Thread is still running...");
                return;
            }

            printThread = new Print_Thread(PRINT_TEXT);
            printThread.start();
        }
    }

    public void onClickPrintOpen() {
        if (printerStatus == 0) {
            Toast.makeText(getApplicationContext(), "Turn printer ON to continue!!!",
                    Toast.LENGTH_SHORT).show();
        } else {
            if (printThread != null && printThread.isThreadFinished()) {
                Log.e(tag, "Thread is still running...");
                return;
            }

            printThread = new Print_Thread(PRINT_OPEN);
            printThread.start();
        }
    }

    MainActivity.Print_Thread printThread = null;

    public class Print_Thread extends Thread {

        int type;

        public boolean isThreadFinished() {
            return !m_bThreadFinished;
        }

        public Print_Thread(int type) {
            this.type = type;
        }

        public void run() {
            Log.d("Print_Thread[ run ]", "run() begin");
            Message msg = Message.obtain();
            Message msg1 = new Message();

            synchronized (this) {

                m_bThreadFinished = false;
                try {
                    ret = posApiHelper.PrintInit();
                } catch (PrintInitException e) {
                    e.printStackTrace();
                    int initRet = e.getExceptionCode();
                    Log.e(tag, "initRer : " + initRet);
                }

                Log.e(tag, "init code:" + ret);

                ret = getValue();
                Log.e(tag, "getValue():" + ret);

                posApiHelper.PrintSetGray(ret);

                //posApiHelper.PrintSetVoltage(BatteryV * 2 / 100);

                ret = posApiHelper.PrintCheckStatus();
                if (ret == -1) {
                    RESULT_CODE = -1;
                    Log.e(tag, "Lib_PrnCheckStatus fail, ret = " + ret);
                    SendMsg("Error, No Paper!!");
                    m_bThreadFinished = true;
                    return;
                } else if (ret == -2) {
                    RESULT_CODE = -1;
                    Log.e(tag, "Lib_PrnCheckStatus fail, ret = " + ret);
                    SendMsg("Error, Printer Too Hot ");
                    m_bThreadFinished = true;
                    return;
                } else if (ret == -3) {
                    RESULT_CODE = -1;
                    Log.e(tag, "voltage = " + (BatteryV * 2));
//                    SendMsg("Battery less :" + (BatteryV * 2));
                    SendMsg("Battery is " + voltage_level + "%" + " Connect to power");
                    //System.out.println("Battery less :" + (BatteryV * 2));
                    m_bThreadFinished = true;
                    return;
                }
                /* else if (voltage_level < 5) {
                    RESULT_CODE = -1;
                    Log.e(tag, "voltage_level = " + voltage_level);
                    SendMsg("Battery capacity less : " + voltage_level);
                    m_bThreadFinished = true;
                    return;
                }*/
                else {
                    RESULT_CODE = 0;
                }

                switch (type) {
                    case PRINT_TEXT:
                        msg.what = DISABLE_RG;
                        handler.sendMessage(msg);
                        /** Font Options
                         *
                         * the font type is IBMPlexMono:
                         * posApiHelper.PrintSetFont((byte) 20, (byte) 20, (byte) 0x00);
                         * posApiHelper.PrintSetFont((byte) 28, (byte) 28, (byte) 0x00);
                         *
                         * the font type is "SHTRIH-FR:
                         * posApiHelper.PrintSetFont((byte) 16, (byte) 16, (byte) 0x00);
                         * posApiHelper.PrintSetFont((byte) 24, (byte) 24, (byte) 0x00);
                         *
                         * Zoom：
                         * Font set as bold and bigger, value 0x00 or 0x33
                         */
                        posApiHelper.PrintSetFont((byte) 26, (byte) 26, (byte) 0x00);
                        posApiHelper.PrintStr(text + "\n");
                        posApiHelper.PrintStr("        \n");
                        posApiHelper.PrintStr("        \n");
                        posApiHelper.PrintStr("        \n");
                        posApiHelper.PrintStr("        \n");
                        posApiHelper.PrintStr("        \n");
                        posApiHelper.PrintStr("        \n");

                        ret = posApiHelper.PrintStart();
                        msg1.what = ENABLE_RG;
                        handler.sendMessage(msg1);

                        Log.d("", "Lib_PrnStart ret = " + ret);

                        if (ret != 0) {
                            RESULT_CODE = -1;
                            Log.e("PrismApp", "Lib_PrnStart fail, ret = " + ret);
                            if (ret == -1) {
                                SendMsg("No Print Paper ");
                            } else if(ret == -2) {
                                SendMsg("too hot");
                            }else if(ret == -3) {
                                SendMsg("low voltage");
                            }else{
                                SendMsg("Print fail");
                            }
                        } else {
                            RESULT_CODE = 0;
                            SendMsg("Print Finish");
                        }

                        break;

                    case PRINT_OPEN:
                        try {
                            ret = posApiHelper.PrintOpen();
                        } catch (PrintInitException e) {
                            e.printStackTrace();
                        }
                        Log.d("", "Lib_PrnStart ret = " + ret);
                        if (ret != 0) {
                            RESULT_CODE = -1;
                            Log.e("PrismApp", "Lib_PrnStart fail, ret = " + ret);
                            if (ret == -1) {
                                SendMsg("No Print Paper ");
                            } else if(ret == -2) {
                                SendMsg("too hot");
                            }else if(ret == -3) {
                                SendMsg("low voltage");
                            }else{
                                SendMsg("Print fail");
                            }
                        } else {
                            RESULT_CODE = 0;
                            SendMsg("Print Finish");
                        }

                        break;
                    default:
                        break;
                }
                m_bThreadFinished = true;

                Log.e(tag, "goToSleep2...");
            }
        }
    }

    /*Sets up the density of printing
    * Default mode is 3*/
    private int getValue() {
        if (enteredText.isEmpty()) {
            sp = getSharedPreferences("Gray", MODE_PRIVATE);
            return sp.getInt("value", 3);
        } else {
            sp = getSharedPreferences("Gray", MODE_PRIVATE);
            return sp.getInt("value", Integer.parseInt(enteredText));
        }
    }

    /*Handles catching of error messages from print process*/
    public void SendMsg(String strInfo) {
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("MSG", strInfo);
        msg.setData(b);
        handler.sendMessage(msg);
    }
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case DISABLE_RG:
                    IsWorking = 1;
                    break;

                case ENABLE_RG:
                    IsWorking = 0;
                    break;

                default:
                    Bundle b = msg.getData();
                    final String strInfo = b.getString("MSG");
                    TextView textViewMsg = findViewById(R.id.textView3);
                    textViewMsg.setText(strInfo);
                    break;
            }
        }
    };

    /*Handles Battery functionality*/
    public class BatteryReceiver extends BroadcastReceiver {
//        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        public void onReceive(Context context, Intent intent) {
            voltage_level = Objects.requireNonNull(intent.getExtras()).getInt("level");
            //System.out.println("Battery shitOne" + voltage_level);
            //Log.e("wbw", "current  = " + voltage_level);
            BatteryV = intent.getIntExtra("voltage", 0);
            //System.out.println("Battery shitTwo" + BatteryV);
            /*Log.e("wbw", "BatteryV  = " + BatteryV);
            Log.e("wbw", "V  = " + BatteryV * 2 / 100);*/
            //	m_voltage = (int) (65+19*voltage_level/100);
            //   Log.e("wbw","m_voltage  = " + m_voltage );
        }
    }

    /** Tester Sample */
    public void testApiSample(MenuItem item) {
        PosApiHelper.getInstance().SysLogSwitch((byte)1);
        PosApiHelper.getInstance().PrintSetVoltage(75);
        my_ret = PosApiHelper.getInstance().PrintCheckStatus();
        if (my_ret == 0) {
            my_ret = posApiHelper.PrintInit(2, 24, 24, 0x33);
            Log.e("PrintInit", "testApiSample: return value is " + my_ret);
        } else if (my_ret == -1) {
            Log.e("PrintCheckStatus", "testApiSample: Need paper");
        } else if (my_ret == -2) {
            Log.e("PrintCheckStatus", "testApiSample: High printer temperatures");
        } else if (my_ret == -3) {
            Log.e("PrintCheckStatus", "testApiSample: Low battery voltage");
        } else {
            Log.e("PrintCheckStatus", "testApiSample: return value is " + my_ret);
        }
        /*posApiHelper.PrintStr("Print Tile\n");
        if(ret!=0){
            return;
        }
        posApiHelper.PrintStr("- - - - - - - - - - - - - - - - - - - - - - - -\n");
        posApiHelper.PrintStr(" Print Str2 \n");
        posApiHelper.PrintBarcode("123456789", 360, 120, BarcodeFormat.CODE_128);
        posApiHelper.PrintBarcode("123456789", 240, 240, BarcodeFormat.QR_CODE);
        posApiHelper.PrintStr("CODE_128 : " + "123456789" + "\n\n");
        posApiHelper.PrintStr("QR_CODE : " + "123456789" + "\n\n");
        posApiHelper.PrintStr("                              \n");
        int my_return = posApiHelper.PrintStart();
        Log.e("PrintStart", "testApiSample: return value is " + my_return);*/
    }

    /** App whitelist */
    private void AppWhiteListConfig() {
        File f = new File("/data/apkins/package_ins_cfg");
        if (!f.canWrite()) {
            Log.i("AppWhiteListConfig", "file cannot be written!!");
            return;
        }
        try {
            FileWriter fileWriter = new FileWriter(f);
            String s ="com.printsample.v2";
            fileWriter.write(s);
            fileWriter.close();
            Log.i("AppWhiteListConfig", "file written!!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}