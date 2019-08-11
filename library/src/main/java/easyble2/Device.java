package easyble2;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * BLE设备实体类
 * <p>
 * date: 2019/8/3 00:08
 * author: zengfansheng
 */
public class Device implements Comparable<Device>, Cloneable, Parcelable {
    private final BluetoothDevice originDevice;
    @Nullable
    private ScanResult scanResult;
    @NonNull
    String name;
    @NonNull
    final String address;
    int rssi;
    int connectionState;

    public Device(@NonNull BluetoothDevice originDevice) {
        this.originDevice = originDevice;
        this.name = originDevice.getName() == null ? "" : originDevice.getName();
        this.address = originDevice.getAddress();
    }

    @NonNull
    public BluetoothDevice getOriginDevice() {
        return originDevice;
    }

    @Nullable
    public ScanResult getScanResult() {
        return scanResult;
    }

    public void setScanResult(@Nullable ScanResult scanResult) {
        this.scanResult = scanResult;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getAddress() {
        return address;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public int getConnectionState() {
        return connectionState;
    }

    public void setConnectionState(int connectionState) {
        this.connectionState = connectionState;
    }

    @Nullable
    public Boolean isConnectable() {
        if (scanResult != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return scanResult.isConnectable();
            }
        }
        return null;
    }
    
    /**
     * 是否已连接并成功发现服务
     */
    public boolean isConnected() {
        return connectionState == Connection.STATE_SERVICE_DISCOVERED;
    }

    /**
     * 是否已断开连接
     */
    public boolean isDisconnected() {
        return connectionState == Connection.STATE_DISCONNECTED || connectionState == Connection.STATE_RELEASED;
    }

    /**
     * 是否正在连接
     */
    public boolean isConnecting() {
        return connectionState != Connection.STATE_DISCONNECTED && connectionState != Connection.STATE_SERVICE_DISCOVERED &&
                connectionState != Connection.STATE_RELEASED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Device)) return false;

        Device device = (Device) o;

        return address.equals(device.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public int compareTo(@NonNull Device other) {
        if (rssi == 0) {
            return -1;
        } else if (other.rssi == 0) {
            return 1;
        } else {
            int result = Integer.compare(other.rssi, rssi);
            if (result == 0) {
                result = name.compareTo(other.name);
            }
            return result;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.originDevice, flags);
        dest.writeParcelable(this.scanResult, flags);
        dest.writeString(this.name);
        dest.writeString(this.address);
        dest.writeInt(this.rssi);
        dest.writeInt(this.connectionState);
    }

    protected Device(Parcel in) {
        this.originDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
        this.scanResult = in.readParcelable(ScanResult.class.getClassLoader());
        String inName = in.readString();
        this.name = inName == null ? "" : inName;
        this.address = Objects.requireNonNull(in.readString());
        this.rssi = in.readInt();
        this.connectionState = in.readInt();
    }

    public static final Parcelable.Creator<Device> CREATOR = new Parcelable.Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel source) {
            return new Device(source);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };
}
