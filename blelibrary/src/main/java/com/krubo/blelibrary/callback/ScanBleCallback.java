package com.krubo.blelibrary.callback;

import android.bluetooth.BluetoothDevice;

/**
 * @author krubo
 * @created 2017/8/22
 */

public interface ScanBleCallback {

    void scan(BluetoothDevice device);

}
