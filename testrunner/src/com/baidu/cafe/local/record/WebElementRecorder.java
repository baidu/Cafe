package com.baidu.cafe.local.record;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import org.json.JSONObject;

import android.os.SystemClock;
import android.view.View;
import android.webkit.JsPromptResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
    private ViewRecorder                 viewRecorder                    = null;
    private WebView                      webView                         = null;
    private WebElementRecordClient       webElementRecordClient          = null;
    private WebElementEventCreator       webElementEventCreator          = null;
    private final static int             TIMEOUT_NEXT_EVENT              = 500;

    /**
     * lock for OutputEventQueue
     */
    private static String                mSyncWebElementRecordEventQueue = new String(
                                                                                 "mSyncWebElementRecordEventQueue");

    private Queue<WebElementRecordEvent> mWebElementRecordEventQueue     = new LinkedList<WebElementRecordEvent>();

    public WebElementRecorder(ViewRecorder viewRecorder) {
        this.viewRecorder = viewRecorder;
        this.webElementEventCreator = new WebElementEventCreator(viewRecorder);
        this.webElementRecordClient = new WebElementRecordClient(viewRecorder.getLocalLib(),
                webElementEventCreator);
        this.mWebElementRecordEventQueue.clear();
    }

    public boolean offerWebElementRecordEventQueue(WebElementRecordEvent event) {
        synchronized (mSyncWebElementRecordEventQueue) {
            return mWebElementRecordEventQueue.offer(event);
        }
    }

    public WebElementRecordEvent peekWebElementRecordEventQueue() {
        synchronized (mSyncWebElementRecordEventQueue) {
            return mWebElementRecordEventQueue.peek();
        }
    }

    public WebElementRecordEvent pollWebElementRecordEventQueue() {
        synchronized (mSyncWebElementRecordEventQueue) {
            return mWebElementRecordEventQueue.poll();

        }
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (Exception ignored) {
        }
    }

    /**
     * set hook listener on WebView body
     * 
     * @param webView
     * @return
     */
    public void handleWebView(final WebView webView) {
        if (webView == null) {
            return;
        }
        print("start monitor WebView: " + webView);
        this.webView = webView;
        // monitor WebView
        hookWebView(webView);
        handleWebElementRecordEventQueue();
    }

    public void handleWebElementRecordEventQueue() {
        new Thread(new Runnable() {
            public void run() {
                ArrayList<WebElementRecordEvent> events = new ArrayList<WebElementRecordEvent>();
                while (true) {
                    WebElementRecordEvent lastRecordEvent = null;
                    long endTime = System.currentTimeMillis() + TIMEOUT_NEXT_EVENT;
                    WebElementRecordEvent e = null;
                    while (true) {
                        e = null;
                        if ((e = pollWebElementRecordEventQueue()) != null) {
                            // here comes a record event
                            endTime = System.currentTimeMillis() + TIMEOUT_NEXT_EVENT;
                            if (lastRecordEvent == null) {
                                // if e is the first record event
                                events.add(e);
                                lastRecordEvent = e;
                            } else {
                                if (e.time > lastRecordEvent.time
                                        && e.familyString.equals(lastRecordEvent.familyString)) {
                                    events.add(e);
                                    lastRecordEvent = e;
                                } else {
                                    offerOutputEventQueue(events);
                                    lastRecordEvent = null;
                                    events.clear();
                                }
                            }
                        } else {
                            // wait until timeout, then offerOutputEventQueue
                            if (System.currentTimeMillis() > endTime) {
                                offerOutputEventQueue(events);
                                lastRecordEvent = null;
                                events.clear();
                                sleep(50);
                                break;
                            }
                        }
                        sleep(10);
                    }
                }
            }
        }).start();
    }

    private boolean offerOutputEventQueue(ArrayList<WebElementRecordEvent> events) {
        if (events.isEmpty()) {
            return false;
        }
        boolean eventOffered = false;
        boolean isTouchstart = false;
        boolean isTouchmove = false;
        for (WebElementRecordEvent e : events) {
            if ("touchstart".equals(e.action)) {
                isTouchstart = true;
            } else if ("touchmove".equals(e.action)) {
                if (isTouchstart) {
                    isTouchmove = true;
                } else {// touchmove without touchstart before, note as illegal
                    eventOffered = false;
                    break;
                }
            } else if ("touchcancel".equals(e.action)) {
                eventOffered = false;
                break;
            } else if ("touchend".equals(e.action)) {
                // when it comes touchstart -> touchend without touchmove in the
                // list, offer a click action
                if (isTouchstart && !isTouchmove) {
                    eventOffered = true;
                    // break;
                } else {
                    // touchstart -> touchmove -> touchend
                    eventOffered = false;
                    // break;
                }
                break;
            } else if ("click".equals(e.action)) {
                eventOffered = true;
                break;
            }
        }
        if (eventOffered) {
            WebElementRecordEvent e = events.get(0);
            OutputEvent outputEvent = new WebElementClickEvent(e.view);
            outputEvent.setCode(String.format(
                    "local.clickOnWebElementByFamilyString(\"%s\"); // Click On [%s]",
                    e.familyString, e.tag));
            viewRecorder.offerOutputEventQueue(outputEvent);
        }
        return eventOffered;
    }

    public void hookWebView(final WebView webView) {
        webElementEventCreator.prepareForStart();
        webElementRecordClient.setWebElementRecordClient(webView);
        final String javaScript = getJavaScriptAsString();
        hookWebElements(webView, javaScript);
        webView.post(new Runnable() {

            @Override
            public void run() {
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        hookWebElements(view, javaScript);
                    }
                });

            }
        });
    }

    /**
     * @param webView
     * @param javaScript
     */
    public void hookWebElements(final WebView webView, final String javaScript) {
        viewRecorder.getLocalLib().getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (webView != null) {
                    webView.loadUrl("javascript:" + javaScript);
                }
            }
        });
    }

    private String getJavaScriptAsString() {
        StringBuffer javaScript = new StringBuffer();
        try {
            // InputStream fis =
            // getClass().getResourceAsStream("WebElementRecorder.js");
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
        private boolean             isFinished   = false;
        private ViewRecorder        viewRecorder = null;

        public final static boolean DEBUG        = true;

        public WebElementEventCreator(ViewRecorder viewRecorder) {
            this.viewRecorder = viewRecorder;
        }

        public void prepareForStart() {
            setFinished(false);
        }

        public void createWebElementEvent(String information, WebView webView) {
            String action = "";
            long time = 0;
            String familyString = "";
            int x = 0;
            int y = 0;
            int width = 0;
            int height = 0;
            String value = "";
            String tag = "";
            try {
                JSONObject jsonObject = new JSONObject(information);
                action = jsonObject.getString("action");
                time = Long.parseLong(jsonObject.getString("time"));
                familyString = jsonObject.getString("familyString");
                x = Integer.parseInt(jsonObject.getString("left"));
                y = Integer.parseInt(jsonObject.getString("top"));
                width = Integer.parseInt(jsonObject.getString("width"));
                height = Integer.parseInt(jsonObject.getString("height"));
                value = jsonObject.isNull("value") ? "" : jsonObject.getString("value");
                tag = jsonObject.isNull("tag") ? "" : jsonObject.getString("tag");
            } catch (Exception ignored) {
                if (DEBUG) {
                    ignored.printStackTrace();
                }
            }
            float scale = webView.getScale();
            int[] locationOfWebViewXY = new int[2];
            webView.getLocationOnScreen(locationOfWebViewXY);

            int locationX = (int) (locationOfWebViewXY[0] + (x + (Math.floor(width / 2))) * scale);
            int locationY = (int) (locationOfWebViewXY[1] + (y + (Math.floor(height / 2))) * scale);
            System.out.println("[action:" + action + "] [familyString:" + familyString
                    + "] [locationX:" + locationX + "] [locationY:" + locationY + "] [time:" + time
                    + "] [tag:" + tag + "]");
            WebElementRecordEvent event = new WebElementRecordEvent(webView, familyString, action,
                    locationX, locationY, time, value, tag);
            offerWebElementRecordEventQueue(event);
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
                sleep(2);
            }
            return false;
        }
    }

    class WebElementRecordEvent {
        public View   view;
        public String familyString;
        public String action;
        public int    x;
        public int    y;
        public long   time;
        public String value;
        public String tag;

        public WebElementRecordEvent(View view, String familyString, String action, int x, int y,
                long time, String value, String tag) {
            this.view = view;
            this.familyString = familyString;
            this.action = action;
            this.x = x;
            this.y = y;
            this.time = time;
            this.value = value;
            this.tag = tag;
        }
    }

    class WebElementClickEvent extends OutputEvent {
        public WebElementClickEvent(View view) {
            this.view = view;
            this.priority = PRIORITY_WEBELEMENT_CLICK;
        }
    }

    class WebElementChangeEvent extends OutputEvent {
        public WebElementChangeEvent(View view) {
            this.view = view;
            this.priority = PRIORITY_WEBELEMENT_CHANGE;
        }
    }

}
