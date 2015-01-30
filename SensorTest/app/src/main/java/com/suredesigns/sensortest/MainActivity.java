package com.suredesigns.sensortest;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int viewId = R.id.web_view;
        mWebView = (WebView) findViewById(viewId);
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.loadUrl("http://www.amazon.co.uk/dp/0486477223");
        TiltSensorEvent.getInstance().initializeTiltService(findViewById(viewId));
    }

    @Override
    protected void onStart(){
        super.onStart();
        TiltSensorEvent.getInstance().startTiltSensorService();
    }
    @Override
    protected void onStop(){
        super.onStop();
        TiltSensorEvent.getInstance().stopTiltSensorService();
    }
    @Override
    protected void onDestroy() {
        TiltSensorEvent.getInstance().stopTiltSensorService();
        setVisible(false);
        super.onDestroy();
    }
    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                    return true;
                } else {
                    return super.onKeyDown(keyCode, event);
                }
            case KeyEvent.KEYCODE_FORWARD:
                if (mWebView.canGoForward()) mWebView.goForward();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        TiltSensorEvent.getInstance().defaultOnWindowFocusChanged(hasFocus);
    }
}