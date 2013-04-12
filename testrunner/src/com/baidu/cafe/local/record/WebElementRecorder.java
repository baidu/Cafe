package com.baidu.cafe.local.record;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.os.SystemClock;
import android.view.View;
import android.webkit.JsPromptResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.baidu.cafe.CafeTestCase;
import com.baidu.cafe.local.DESEncryption;
import com.baidu.cafe.local.LocalLib;
import com.baidu.cafe.local.Log;

/**
 * one object one webview
 * 
 * @author leiming@baidu.com
 * @date 2013-4-12
 * @version
 * @todo
 */
public class WebElementRecorder {
    private ViewRecorder           viewRecorder           = null;
    private WebView                webView                = null;
    private WebElementRecordClient webElementRecordClient = null;
    private WebElementEventCreator webElementEventCreator = null;

    public WebElementRecorder(ViewRecorder viewRecorder) {
        this.viewRecorder = viewRecorder;
        this.webElementEventCreator = new WebElementEventCreator(viewRecorder);
        this.webElementRecordClient = new WebElementRecordClient(viewRecorder.getLocalLib(),
                webElementEventCreator);
    }

    /**
     * set hook listener on WebView body
     * 
     * @param webView
     * @return
     */
    public boolean handleWebView(final WebView webView) {
        if (webView == null) {
            return false;
        }
        this.webView = webView;
        webElementEventCreator.prepareForStart();
        webElementRecordClient.setWebElementRecordClient(webView);
        final String javaScript = getJavaScriptAsString();
        viewRecorder.getLocalLib().getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (webView != null) {
                    webView.loadUrl("javascript:" + javaScript);
                }
            }
        });
        return true;
    }

    private String getJavaScriptAsString() {
        StringBuffer javaScript = new StringBuffer();
        try {
            //            InputStream fis = getClass().getResourceAsStream("WebElementRecorder.js");
            InputStream fis = new FileInputStream(CafeTestCase.mTargetFilesDir
                    + "/WebElementRecorder.js");
            BufferedReader input = new BufferedReader(new InputStreamReader(fis));
            String line = null;
            while ((line = input.readLine()) != null) {
                javaScript.append(line);
                javaScript.append("\n");
            }
            input.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return javaScript.toString();
    }

    class WebElementRecordClient extends WebChromeClient {
        LocalLib                local;
        WebElementEventCreator  webElementEventCreator;
        private WebChromeClient webElementRecordClient;

        /**
         * Constructs this object.
         * 
         * @param instrumentation
         *            the {@code Instrumentation} instance
         * @param webElementCreator
         *            the {@code WebElementCreator} instance
         */

        public WebElementRecordClient(LocalLib local, WebElementEventCreator webElementEventCreator) {
            this.local = local;
            this.webElementEventCreator = webElementEventCreator;
            webElementRecordClient = this;
        }

        /**
         * Enables JavaScript in the given {@code WebViews} objects.
         * 
         * @param webViews
         *            the {@code WebView} objects to enable JavaScript in
         */

        public void setWebElementRecordClient(final WebView webView) {
            if (webView != null) {
                local.runOnMainSync(new Runnable() {
                    @Override
                    public void run() {
                        webView.getSettings().setJavaScriptEnabled(true);
                        webView.setWebChromeClient(webElementRecordClient);
                    }
                });
            }
        }

        /**
         * Overrides onJsPrompt in order to create {@code WebElement} objects
         * based on the web elements attributes prompted by the injections of
         * JavaScript
         */

        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue,
                JsPromptResult r) {

            if (message != null) {
                if (message.equals("WebElementRecorder-finished")) {
                    webElementEventCreator.setFinished(true);
                } else {
                    webElementEventCreator.createWebElementEvent(message, view);
                }
            }
            r.confirm();
            return true;
        }

    }

    private void print(String message) {
        if (ViewRecorder.DEBUG) {
            Log.i("ViewRecorder", message);
        } else {
            Log.i("ViewRecorder", DESEncryption.encryptStr(message));
        }
    }

    class WebElementEventCreator {
        private boolean      isFinished   = false;
        private ViewRecorder viewRecorder = null;

        public WebElementEventCreator(ViewRecorder viewRecorder) {
            this.viewRecorder = viewRecorder;
        }

        public void prepareForStart() {
            setFinished(false);
        }

        public void createWebElementEvent(String information, WebView webView) {
            String[] data = information.split(";,");
            String type = data[0];
            String code = data[1];
            WebElementEvent event = null;
            if ("click".equals(type)) {
                event = new WebElementClickEvent(webView);
                event.setCode(code);
            } else if ("change".equals(type)) {
                event = new WebElementChangeEvent(webView);
                event.setCode(code);
            } else {
                event = null;
            }

            if (event != null) {
                print("!!!!!!!!!!!!WebElementEventCreator:" + event);
                viewRecorder.offerOutputEventQueue(event);
            }
        }

        public void setFinished(boolean isFinished) {
            this.isFinished = isFinished;
        }

        public boolean isFinished() {
            return isFinished;
        }

        private boolean waitForWebElementsToBeCreated() {
            final long endTime = SystemClock.uptimeMillis() + 20;

            while (SystemClock.uptimeMillis() < endTime) {

                if (isFinished) {
                    return true;
                }
                try {
                    Thread.sleep(2);
                } catch (InterruptedException ignored) {
                }
            }
            return false;
        }
    }

    class WebElementEvent extends OutputEvent {

    }

    class WebElementClickEvent extends WebElementEvent {
        public WebElementClickEvent(View view) {
            this.view = view;
            this.priority = PRIORITY_WEBELEMENT_CLICK;
        }
    }

    class WebElementChangeEvent extends WebElementEvent {
        public WebElementChangeEvent(View view) {
            this.view = view;
            this.priority = PRIORITY_WEBELEMENT_CHANGE;
        }
    }

}
