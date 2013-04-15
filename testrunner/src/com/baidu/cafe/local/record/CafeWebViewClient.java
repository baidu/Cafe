package com.baidu.cafe.local.record;

import android.graphics.Bitmap;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * @author leiming@baidu.com
 * @date 2013-4-15
 * @version
 * @todo
 */
public class CafeWebViewClient extends WebViewClient {
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        view.loadUrl(url);
        return true;
    }

    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        Log.i("WebView", "onPageStarted");
        super.onPageStarted(view, url, favicon);
    }

    public void onPageFinished(WebView view, String url) {
        Log.i("WebView", "onPageFinished ");
        view.loadUrl("javascript:console.log(document.getElementsByTagName('html')[0].innerHTML);");
        super.onPageFinished(view, url);
    }
}
