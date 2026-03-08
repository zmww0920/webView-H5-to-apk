package com.example.test;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private final int PICK_REQUEST = 10001;
    ValueCallback<Uri> mFilePathCallback;
    ValueCallback<Uri[]> mFilePathCallbackArray;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏ActionBar
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_main);
        // WebView加载页面
        webView = findViewById(R.id.web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        // 处理文件上传
        webView.setWebChromeClient(new WebChromeClient() {
            // Android 4.1----4.4
            public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType, String capture) {
                mFilePathCallback = uploadFile;
                handle(uploadFile);
            }

            // for 5.0+
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (mFilePathCallbackArray != null) {
                    mFilePathCallbackArray.onReceiveValue(null);
                }
                mFilePathCallbackArray = filePathCallback;
                handleup(filePathCallback);
                return true;
            }

            private void handle(ValueCallback<Uri> uploadFile) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("*/*");
                startActivityForResult(intent, PICK_REQUEST);
            }

            private void handleup(ValueCallback<Uri[]> uploadFile) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("*/*");
                startActivityForResult(intent, PICK_REQUEST);
            }
        });

        // 监听下载事件
        webView.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("下载中...");
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype));

                DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                manager.enqueue(request);
                showMessage("下载中...");

                BroadcastReceiver onComplete = new BroadcastReceiver() {
                    public void onReceive(Context ctxt, Intent intent) {
                        showMessage("下载完成");
                        unregisterReceiver(this);
                    }
                };
                registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            }
        });

        // 不调用系统浏览器
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        // 加载你的进销存系统
        webView.loadUrl("http://www.zmww0920.dpdns.org:8010");

        // 显示小图片、开启本地存储等配置
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
    }

    // 回退页面
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Deprecated
    public void showMessage(String _s) {
        Toast.makeText(getApplicationContext(), _s, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_REQUEST) {
            if (data != null) {
                Uri uri = data.getData();
                handleCallback(uri);
            } else {
                handleCallback(null);
            }
        } else {
            handleCallback(null);
        }
    }

    /**
     * 处理WebView的回调
     */
    private void handleCallback(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mFilePathCallbackArray != null) {
                if (uri != null) {
                    mFilePathCallbackArray.onReceiveValue(new Uri[]{uri});
                } else {
                    mFilePathCallbackArray.onReceiveValue(null);
                }
                mFilePathCallbackArray = null;
            }
        } else {
            if (mFilePathCallback != null) {
                if (uri != null) {
                    String url = getFilePathFromContentUri(uri, getContentResolver());
                    Uri u = Uri.fromFile(new File(url));
                    mFilePathCallback.onReceiveValue(u);
                } else {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = null;
            }
        }
    }

    public static String getFilePathFromContentUri(Uri selectedVideoUri, ContentResolver contentResolver) {
        String filePath;
        String[] filePathColumn = {MediaStore.MediaColumns.DATA};
        Cursor cursor = contentResolver.query(selectedVideoUri, filePathColumn, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            filePath = cursor.getString(columnIndex);
            cursor.close();
        } else {
            filePath = selectedVideoUri.getPath();
        }
        return filePath;
    }
}
