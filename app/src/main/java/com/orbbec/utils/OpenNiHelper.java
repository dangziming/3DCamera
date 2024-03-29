package com.orbbec.utils;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

public class OpenNiHelper {

    static{
        System.loadLibrary("usb");
        System.loadLibrary("OpenNI2");
    }

    private static final int OB_VID = 0x2BC5;
    private static final int OB_START_PID = 0x0401;
    private static final int OB_END_PID = 0x0410;
    private DeviceOpenListener mDeviceOpenListener;
    private Context mAndroidContext;
    private String mActionUsbPermission;
    private static final String OPENNI_ASSETS_DIR = "openni";
    private UsbDeviceConnection mUsbDeviceConnection;

    public OpenNiHelper(Context context) {
        mAndroidContext = context;

        /*
         * The configuration files are saved as assets. To make them readable by
         * the OpenNI native library, we need to write them to the application
         * files directory
         */
        try {
            for (String fileName : mAndroidContext.getAssets().list(OPENNI_ASSETS_DIR)) {
                extractOpenNIAsset(fileName);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mActionUsbPermission = context.getPackageName() + ".USB_PERMISSION";

        IntentFilter filter = new IntentFilter(mActionUsbPermission);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mAndroidContext.registerReceiver(mUsbReceiver, filter);
    }


    public static interface DeviceOpenListener {
        /**
         * Called when permission to access the device is granted.
         *
         * @param device The device for which permission was granted.
         */
        public abstract void onDeviceOpened(UsbDevice device);


        /**
         * Called when permission is access the device is denied.
         *
         * @param device
         */
        public abstract void onDeviceOpenFailed(UsbDevice device);
    }



    public static boolean hasObUsbDevice(Context context) {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> iterator = deviceList.values().iterator();
        // Remove any device that is not known to be OpenNI-compliant from the
        // list of USB devices
        while (iterator.hasNext()) {
            UsbDevice usbDevice = iterator.next();
            //Log.v(TAG, "vid: " + Integer.toHexString(usbDevice.getVendorId()) + ", pid: " + Integer.toHexString(usbDevice.getProductId()));
            boolean obDevice = usbDevice.getVendorId() == OB_VID && (usbDevice.getProductId() <= OB_END_PID && usbDevice.getProductId() >= OB_START_PID);
            if (obDevice) {
                return true;
            }
        }
        return false;
    }

    public void requestDeviceOpen(DeviceOpenListener listener) {
        // Theoretically, the client may call this method more than once with different listeners.
        mDeviceOpenListener = listener;

        // check if this is a USB device
        UsbDevice usbDevice = getUsbDevice();

        if (usbDevice == null) {
            // not a USB device, just open it
            Toast.makeText(mAndroidContext, "UsbManager findn't orbbec device", Toast.LENGTH_SHORT).show();
        } else {
            // USB device. request permissions for it
            PendingIntent permissionIntent = PendingIntent.getBroadcast(mAndroidContext, 0, new Intent(
                    mActionUsbPermission), 0);

            UsbManager manager = (UsbManager) mAndroidContext.getSystemService(Context.USB_SERVICE);

            manager.requestPermission(usbDevice, permissionIntent);
            // flow will continue in the intent
        }
    }

    public UsbDevice getUsbDevice() {
        UsbManager manager = (UsbManager) mAndroidContext.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> iterator = deviceList.values().iterator();

        // Remove any device that is not known to be OpenNI-compliant from the
        // list of USB devices

        while (iterator.hasNext()) {
            UsbDevice usbDevice = iterator.next();
           // Log.v(TAG, "vid: " + Integer.toHexString(usbDevice.getVendorId()) + ", pid: " + Integer.toHexString(usbDevice.getProductId()));
            boolean obDevice = usbDevice.getVendorId() == OB_VID && (usbDevice.getProductId() <= OB_END_PID && usbDevice.getProductId() >= OB_START_PID);
            if (obDevice) {
                return usbDevice;
            }
        }

        return null;
    }


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (mActionUsbPermission.equals(action)) {
                synchronized (this) {
                    if (mDeviceOpenListener == null) {
                        return;
                    }

                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device == null) {
                        return;
                    }

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        // permission granted. open the device
                        try {
                            openDevice(device);
                        } catch (Exception ex) {
                            //Log.e(TAG, "Can't open device though permission was granted: " + ex);
                            mDeviceOpenListener.onDeviceOpenFailed(device);
                        }
                    } else {
                       // Log.e(TAG, "Permission denied for device");
                        mDeviceOpenListener.onDeviceOpenFailed(device);
                    }
                }
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                Log.e("edg", "插入 usb");
            } else if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                Log.e("edg", "拔出 usb");
            }
        }
    };

    public void shutdown() {
        try {
            mUsbDeviceConnection.close();
            mAndroidContext.unregisterReceiver(mUsbReceiver);
        } catch (Exception e) {
        }

    }


    private void openDevice(UsbDevice device) {
        try {
            mUsbDeviceConnection = ((UsbManager) mAndroidContext.getSystemService(Context.USB_SERVICE)).openDevice(device);
            if (mUsbDeviceConnection == null) {
                mDeviceOpenListener.onDeviceOpenFailed(device);
            } else {
                mDeviceOpenListener.onDeviceOpened(device);
            }
        } catch (Exception ex) {
            //Log.e(TAG, "Failed to open device:" + ex);
            mDeviceOpenListener.onDeviceOpenFailed(device);
        }
    }

    private void extractOpenNIAsset(String filename) throws IOException {
        InputStream is = mAndroidContext.getAssets().open(OPENNI_ASSETS_DIR + "/" + filename);

        mAndroidContext.deleteFile(filename);
        OutputStream os = mAndroidContext.openFileOutput(filename, Context.MODE_PRIVATE);

        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        os.write(buffer);
        os.close();
    }
}
