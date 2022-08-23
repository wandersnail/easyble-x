package com.wandersnail.bledemo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelUuid;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import cn.wandersnail.ble.Connection;
import cn.wandersnail.ble.ConnectionState;
import cn.wandersnail.ble.Device;
import cn.wandersnail.ble.EasyBLE;
import cn.wandersnail.ble.EventObserver;
import cn.wandersnail.ble.Request;
import cn.wandersnail.ble.RequestBuilderFactory;
import cn.wandersnail.ble.RequestType;
import cn.wandersnail.ble.WriteCharacteristicBuilder;
import cn.wandersnail.ble.WriteOptions;
import cn.wandersnail.commons.helper.PermissionsRequester2;
import cn.wandersnail.commons.poster.RunOn;
import cn.wandersnail.commons.poster.ThreadMode;
import cn.wandersnail.commons.util.FileUtils;
import cn.wandersnail.commons.util.ToastUtils;

/**
 * date: 2022/8/22 17:17
 * author: zengfansheng
 */
public class SendFileActivity extends BaseActivity {
    private TextView tvPath;
    private Button btnSelectFile;
    private Button btnSend;
    private ProgressBar progressBar;
    private TextView tvPercent;
    private TextView tvProgress;
    private TextView tvState;
    private ParcelUuid writeService;
    private ParcelUuid writeCharacteristic;
    private Connection connection;
    private final ConcurrentLinkedQueue<byte[]> queue = new ConcurrentLinkedQueue<>();
    private DocumentFile file;
    private File legacyFile;
    private long totalLength;
    private long sentLength;
    private long lastUpdateUiTime;
    private boolean sending;
    private boolean isOldWaySelectFile;
    private final String requestId = UUID.randomUUID().toString();
    private ActivityResultLauncher<Intent> selectFileLauncher;
    
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("发送文件");
        setContentView(R.layout.send_file_activity);
        assignViews();
        Device device = getIntent().getParcelableExtra("DEVICE");
        writeService = getIntent().getParcelableExtra("SERVICE");
        writeCharacteristic = getIntent().getParcelableExtra("CHARACTERISTIC");
        connection = EasyBLE.getInstance().getConnection(device);
        EasyBLE.getInstance().registerObserver(eventObserver);
        progressBar.setMax(10000);
        btnSelectFile.setOnClickListener(v -> {
            if (!doSelect(Intent.ACTION_OPEN_DOCUMENT)) {
                isOldWaySelectFile = true;
                if (!doSelect(Intent.ACTION_GET_CONTENT)) {
                    ToastUtils.showShort("操作失败！当前系统缺少文件选择组件！");
                }
            }
        });
        btnSend.setOnClickListener(v-> {
            btnSend.setEnabled(false);
            progressBar.setProgress(0);
            btnSelectFile.setEnabled(false);
            totalLength = file != null ? file.length() : legacyFile.length();
            sending = true;
            new Thread(()-> {
                InputStream input = null;
                try {
                    if (file != null) {
                        input = getContentResolver().openInputStream(file.getUri());
                    } else {
                        input = new FileInputStream(legacyFile);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (input == null) {
                    runOnUiThread(()-> {
                        sending = false;
                        btnSend.setEnabled(true);
                        btnSelectFile.setEnabled(true);
                        ToastUtils.showShort("文件打开失败");
                    });
                    return;
                }
                int packageSize = connection.getMtu() - 3;
                byte[] buf = new byte[packageSize];
                try {
                    int len = input.read(buf);
                    if (len != -1) {
                        //先发第一包，成功回调后会从队列取出继续发送
                        WriteCharacteristicBuilder builder = new RequestBuilderFactory().getWriteCharacteristicBuilder(
                                writeService.getUuid(),
                                writeCharacteristic.getUuid(),
                                Arrays.copyOf(buf, len)
                        ).setTag(requestId);
                        builder.setWriteOptions(new WriteOptions.Builder()
                                .setPackageSize(len)
                                .build());
                        connection.execute(builder.build());
                    }
                    while (sending && len != -1) {
                        if (queue.size() > 500) {
                            Thread.sleep(100);
                        } else {
                            queue.add(Arrays.copyOf(buf, len));
                            len = input.read(buf);
                        }
                    }
                    input.close();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        });
        selectFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                if (isOldWaySelectFile) {
                    String path = FileUtils.getFileRealPath(this, result.getData().getData());
                    if (path != null) {
                        File f = new File(path);
                        if (!f.exists()) {
                            ToastUtils.showShort("文件不存在");
                        } else if (updateFileInfo(f.getAbsolutePath(), f.length())) {
                            legacyFile = f;
                        }
                    }
                } else {
                    DocumentFile file = DocumentFile.fromSingleUri(this, result.getData().getData());
                    if (file != null) {
                        if (!file.exists()) {
                            ToastUtils.showShort("文件不存在");
                        } else {
                            String path = FileUtils.getFileRealPath(this, result.getData().getData());
                            if (path == null) {
                                path = result.getData().getData().toString();
                            }
                            if (updateFileInfo(path, file.length())) {
                                this.file = file;
                            }
                        }
                    }
                }
            }
        });

        //动态申请权限
        PermissionsRequester2 permissionsRequester = new PermissionsRequester2(this);
        List<String> list = new ArrayList<>();
        list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionsRequester.checkAndRequest(list);
    }

    private void assignViews() {
        tvPath = findViewById(R.id.tvPath);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnSend = findViewById(R.id.btnSend);
        progressBar = findViewById(R.id.progressBar);
        tvPercent = findViewById(R.id.tvPercent);
        tvProgress = findViewById(R.id.tvProgress);
        tvState = findViewById(R.id.tvState);
    }
    
    private boolean updateFileInfo(String path, long len) {
        if (len <= 0) {
            ToastUtils.showShort("请选择非空文件");
            return false;
        }
        String root = "";
        try {
            root = Environment.getExternalStorageDirectory().getAbsolutePath();
        } catch (Throwable ignore) {}
        tvPath.setText(root.length() > 0 ? path.replace(root, "内部存储") : path);
        btnSend.setEnabled(true);
        totalLength = len;
        sentLength = 0;
        tvState.setText("");
        updateProgress();
        return true;
    }
    
    private final EventObserver eventObserver = new EventObserver() {
        @RunOn(ThreadMode.MAIN)
        @Override
        public void onConnectionStateChanged(@NonNull Device device) {
            if (device.getConnectionState() != ConnectionState.SERVICE_DISCOVERED) {
                tvState.setText("连接断开");
                sending = false;
            }
        }

        @RunOn(ThreadMode.BACKGROUND)
        @Override
        public void onCharacteristicWrite(@NonNull Request request, @NonNull byte[] value) {
            if (sending && requestId.equals(request.getTag())) {
                sentLength += value.length;
                if (queue.isEmpty()) {
                    runOnUiThread(()-> tvState.setText("发送完成"));
                } else {
                    byte[] bytes = queue.remove();
                    WriteCharacteristicBuilder builder = new RequestBuilderFactory().getWriteCharacteristicBuilder(
                            writeService.getUuid(),
                            writeCharacteristic.getUuid(),
                            bytes
                    ).setTag(requestId);
                    builder.setWriteOptions(new WriteOptions.Builder()
                            .setPackageSize(bytes.length)
                            .build());
                    connection.execute(builder.build());
                }
                updateProgress();
            }
        }

        @RunOn(ThreadMode.MAIN)
        @Override
        public void onRequestFailed(@NonNull Request request, int failType, int gattStatus, @Nullable Object value) {
            if (sending && requestId.equals(request.getTag()) && request.getType() == RequestType.WRITE_CHARACTERISTIC) {
                sending = false;
                btnSend.setEnabled(true);
                btnSelectFile.setEnabled(true);
                tvState.setText("发送失败");
            }
        }
    };
    
    private boolean doSelect(String action) {
        try {
            //type和category都必须写，否则无法调起，还会抛异常
            Intent intent = new Intent(action);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType( "*/*");
            selectFileLauncher.launch(intent);
        } catch (Throwable t) {
            return false;
        }
        return true;
    }
    
    private void updateProgress() {
        if (System.currentTimeMillis() - lastUpdateUiTime < 200) {
            return;
        }
        lastUpdateUiTime = System.currentTimeMillis();
        runOnUiThread(()-> {
            tvProgress.setText(sentLength + "/" + totalLength);
            float percent = totalLength == 0 ? 0 : sentLength * 1f / totalLength;
            tvPercent.setText(new DecimalFormat("#0.00").format(percent * 100) + "%");
            progressBar.setProgress((int) (percent * progressBar.getMax()));
            if (totalLength > 0 && totalLength <= sentLength) {
                sending = false;
                btnSend.setEnabled(true);
                btnSelectFile.setEnabled(true);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EasyBLE.getInstance().unregisterObserver(eventObserver);
    }
}
