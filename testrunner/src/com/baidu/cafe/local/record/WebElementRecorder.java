/*
 * Copyright (C) 2013 Baidu.com Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.cafe.local.record;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import org.json.JSONObject;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.HttpAuthHandler;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebResourceResponse;
import android.webkit.WebStorage.QuotaUpdater;
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
	private ViewRecorder viewRecorder = null;
	private WebView webView = null;
	private WebElementRecordClient webElementRecordClient = null;
	private WebElementEventCreator webElementEventCreator = null;
	private final static int TIMEOUT_NEXT_EVENT = 500;

	/**
	 * lock for OutputEventQueue
	 */
	private static String mSyncWebElementRecordEventQueue = new String("mSyncWebElementRecordEventQueue");

	private Queue<WebElementRecordEvent> mWebElementRecordEventQueue = new LinkedList<WebElementRecordEvent>();

	public WebElementRecorder(ViewRecorder viewRecorder) {
		this.viewRecorder = viewRecorder;
		this.webElementEventCreator = new WebElementEventCreator(viewRecorder);
		this.webElementRecordClient = new WebElementRecordClient(viewRecorder.getLocalLib(), webElementEventCreator);
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
		webView.post(new Runnable() {

			public void run() {
				print("webView getURL: " + webView.getUrl());
			}

		});
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
			outputEvent.setCode(String.format("local.clickOnWebElementByFamilyString(\"%s\"); // Click On [%s]",
					e.familyString, e.tag));
			viewRecorder.offerOutputEventQueue(outputEvent);
		}
		return eventOffered;
	}

	/**
	 * @param webView
	 * @return
	 */
	private WebViewClient getOriginalWebViewClient(final WebView webView) {
		// get WebView class level
		int levelFromWebView = viewRecorder.getLocalLib().countLevelFromViewToFather(webView, WebView.class);
		// print("levelFromWebView:" + levelFromWebView);

		// save old WebViewClient
		WebViewClient mWebViewClient = null;
		try {
			//print("Build.VERSION.SDK_INT : " + Build.VERSION.SDK_INT);
			if (Build.VERSION.SDK_INT > 14) {
				Object originalWebViewClassic = viewRecorder.getLocalLib().invokeObjectMethod(webView,
						levelFromWebView, "getWebViewProvider", new Class[] {}, new Object[] {});
				Object originalCallbackProxy = viewRecorder.getLocalLib().getObjectProperty(originalWebViewClassic, 0,
						"mCallbackProxy");
				mWebViewClient = (WebViewClient) viewRecorder.getLocalLib().getObjectProperty(originalCallbackProxy, 0,
						"mWebViewClient");
			} else {
				print("getClass:" + webView.getClass());
				mWebViewClient = (WebViewClient) viewRecorder.getLocalLib().invokeObjectMethod(webView,
						levelFromWebView, "getWebViewClient", new Class[] {}, new Object[] {});
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return mWebViewClient;
	}

	public void hookWebView(final WebView webView) {
		webElementEventCreator.prepareForStart();
		webElementRecordClient.setWebElementRecordClient(webView);
		final String javaScript = getJavaScriptAsString();
		hookWebElements(webView, javaScript);
		webView.post(new Runnable() {
			// @Override
			public void run() {
				final WebViewClient orginalWebViewClient = getOriginalWebViewClient(webView);
				if (orginalWebViewClient != null) {
					webView.setWebViewClient(new WebViewClient() {

						@Override
						public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
							orginalWebViewClient.doUpdateVisitedHistory(view, url, isReload);
						}

						@Override
						public void onFormResubmission(WebView view, Message dontResend, Message resend) {
							orginalWebViewClient.onFormResubmission(view, dontResend, resend);
						}

						@Override
						public void onLoadResource(WebView view, String url) {
							orginalWebViewClient.onLoadResource(view, url);
						}

						@Override
						public void onPageFinished(WebView view, String url) {
							orginalWebViewClient.onPageFinished(view, url);
							if (url != null) {
								hookWebElements(view, javaScript);
							}
						}

						@Override
						public void onPageStarted(WebView view, String url, Bitmap favicon) {
							orginalWebViewClient.onPageStarted(view, url, favicon);
						}

						@Override
						public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
							orginalWebViewClient.onReceivedError(view, errorCode, description, failingUrl);
						}

						@Override
						public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host,
								String realm) {
							orginalWebViewClient.onReceivedHttpAuthRequest(view, handler, host, realm);
						}

						public void onReceivedLoginRequest(WebView view, String realm, String account, String args) {
							// do support onReceivedLoginRequest since the
							// version 4.0
							if (Build.VERSION.SDK_INT >= 14) {
								orginalWebViewClient.onReceivedLoginRequest(view, realm, account, args);
							}
						}

						@Override
						public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
							orginalWebViewClient.onReceivedSslError(view, handler, error);
						}

						@Override
						public void onScaleChanged(WebView view, float oldScale, float newScale) {
							orginalWebViewClient.onScaleChanged(view, oldScale, newScale);
						}

						@Override
						public void onTooManyRedirects(WebView view, Message cancelMsg, Message continueMsg) {
							orginalWebViewClient.onTooManyRedirects(view, cancelMsg, continueMsg);
						}

						@Override
						public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
							orginalWebViewClient.onUnhandledKeyEvent(view, event);
						}

						@Override
						public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
							if (Build.VERSION.SDK_INT >= 14) {
								return orginalWebViewClient.shouldInterceptRequest(view, url);
							} else {
								return null;
							}
						}

						@Override
						public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
							return orginalWebViewClient.shouldOverrideKeyEvent(view, event);
						}

						@Override
						public boolean shouldOverrideUrlLoading(WebView view, String url) {
							return orginalWebViewClient.shouldOverrideUrlLoading(view, url);
						}

					});
				} else {
					// set hook WebViewClient
					webView.setWebViewClient(new WebViewClient() {
						@Override
						public void onPageFinished(WebView view, String url) {
							super.onPageFinished(webView, url);
							// print("webView onPageFinished: " + url);
							if (url != null) {
								hookWebElements(view, javaScript);
							}
						}
					});
				}
			}
		});

	}

	/**
	 * @param webView
	 * @param javaScript
	 */
	public void hookWebElements(final WebView webView, final String javaScript) {
		webView.post(new Runnable() {
			public void run() {
				if (webView != null) {
					webView.loadUrl("javascript:" + javaScript);
				}
			}
		});
		// viewRecorder.getLocalLib().getCurrentActivity().setProgress(10000);
	}

	private String getJavaScriptAsString() {
		StringBuffer javaScript = new StringBuffer();
		try {
			// InputStream fis =
			// getClass().getResourceAsStream("WebElementRecorder.js");
			InputStream fis = new FileInputStream(CafeTestCase.mTargetFilesDir + "/WebElementRecorder.js");
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
		LocalLib local;
		WebElementEventCreator webElementEventCreator;
		private WebChromeClient webElementRecordClient;

		private WebChromeClient originalWebChromeClient;

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
			originalWebChromeClient = null;
		}

		/**
		 * use java reflect mechanism to get original WebChromeClient object
		 * 
		 * @param webView
		 * @return
		 */
		private WebChromeClient getOriginalWebChromeClient(final WebView webView) {
			WebChromeClient mWebChromeClient = null;
			int levelFromWebView = viewRecorder.getLocalLib().countLevelFromViewToFather(webView, WebView.class);
			try {
				if (Build.VERSION.SDK_INT > 14) {
					Object originalWebViewClassic = viewRecorder.getLocalLib().invokeObjectMethod(webView,
							levelFromWebView, "getWebViewProvider", new Class[] {}, new Object[] {});
					Object originalCallbackProxy = viewRecorder.getLocalLib().getObjectProperty(originalWebViewClassic,
							0, "mCallbackProxy");
					mWebChromeClient = (WebChromeClient) viewRecorder.getLocalLib().getObjectProperty(
							originalCallbackProxy, 0, "mWebChromeClient");
				} else {
					mWebChromeClient = (WebChromeClient) viewRecorder.getLocalLib().invokeObjectMethod(webView,
							levelFromWebView, "getWebChromeClient", new Class[] {}, new Object[] {});
				}
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			}
			return mWebChromeClient;
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
						originalWebChromeClient = getOriginalWebChromeClient(webView);
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
		public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult r) {

			if (message != null) {
				if (message.endsWith("WebElementRecorder-finished")) {
					//Log.i("onJsPrompt : " + message);
					webElementEventCreator.setFinished(true);
				} else {
					webElementEventCreator.createWebElementEvent(message, view);
				}
			}
			r.confirm();
			return true;
		}

		@Override
		public Bitmap getDefaultVideoPoster() {
			if (originalWebChromeClient != null) {
				return originalWebChromeClient.getDefaultVideoPoster();
			} else {
				return super.getDefaultVideoPoster();
			}
		}

		@Override
		public View getVideoLoadingProgressView() {
			if (originalWebChromeClient != null) {
				return originalWebChromeClient.getVideoLoadingProgressView();
			} else {
				return super.getVideoLoadingProgressView();
			}
		}

		@Override
		public void getVisitedHistory(ValueCallback<String[]> callback) {
			if (originalWebChromeClient != null) {
				originalWebChromeClient.getVisitedHistory(callback);
			} else {
				super.getVisitedHistory(callback);
			}
		}

		@Override
		public void onCloseWindow(WebView window) {
			if (originalWebChromeClient != null) {
				originalWebChromeClient.onCloseWindow(window);
			} else {
				super.onCloseWindow(window);
			}
		}

		@Override
		public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
			if (originalWebChromeClient != null) {
				return originalWebChromeClient.onConsoleMessage(consoleMessage);
			} else {
				return super.onConsoleMessage(consoleMessage);
			}
		}

		@Override
		public void onConsoleMessage(String message, int lineNumber, String sourceID) {
			if (originalWebChromeClient != null) {
				originalWebChromeClient.onConsoleMessage(message, lineNumber, sourceID);
			} else {
				super.onConsoleMessage(message, lineNumber, sourceID);
			}
		}

		@Override
		public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
			if (originalWebChromeClient != null) {
				return originalWebChromeClient.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
			} else {
				return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
			}
		}

		@Override
		public void onExceededDatabaseQuota(String url, String databaseIdentifier, long quota,
				long estimatedDatabaseSize, long totalQuota, QuotaUpdater quotaUpdater) {
			if (originalWebChromeClient != null) {
				originalWebChromeClient.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize,
						totalQuota, quotaUpdater);
			} else {
				super.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota,
						quotaUpdater);
			}
		}

		@Override
		public void onGeolocationPermissionsHidePrompt() {
			if (originalWebChromeClient != null) {
				originalWebChromeClient.onGeolocationPermissionsHidePrompt();
			} else {
				super.onGeolocationPermissionsHidePrompt();
			}
		}

		@Override
		public void onGeolocationPermissionsShowPrompt(String origin, Callback callback) {
			if (originalWebChromeClient != null) {
				originalWebChromeClient.onGeolocationPermissionsShowPrompt(origin, callback);
			} else {
				super.onGeolocationPermissionsShowPrompt(origin, callback);
			}
		}

		@Override
		public void onHideCustomView() {
			if (originalWebChromeClient != null) {
				originalWebChromeClient.onHideCustomView();
			} else {
				super.onHideCustomView();
			}
		}

		@Override
		public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
			if (originalWebChromeClient != null) {
				return originalWebChromeClient.onJsAlert(view, url, message, result);
			} else {
				return super.onJsAlert(view, url, message, result);
			}
		}

		@Override
		public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
			if (originalWebChromeClient != null) {
				return originalWebChromeClient.onJsBeforeUnload(view, url, message, result);
			} else {
				return super.onJsBeforeUnload(view, url, message, result);
			}
		}

		@Override
		public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
			if (originalWebChromeClient != null) {
				return originalWebChromeClient.onJsConfirm(view, url, message, result);
			} else {
				return super.onJsConfirm(view, url, message, result);
			}
		}

		@Override
		public boolean onJsTimeout() {
			if (originalWebChromeClient != null) {
				return originalWebChromeClient.onJsTimeout();
			} else {
				return super.onJsTimeout();
			}
		}

		@Override
		public void onProgressChanged(WebView view, int newProgress) {
			if (originalWebChromeClient != null) {
				originalWebChromeClient.onProgressChanged(view, newProgress);
			} else {
				super.onProgressChanged(view, newProgress);
			}
		}

		@Override
		public void onReachedMaxAppCacheSize(long requiredStorage, long quota, QuotaUpdater quotaUpdater) {
			if (originalWebChromeClient != null) {
				originalWebChromeClient.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
			} else {
				super.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
			}
		}

		@Override
		public void onReceivedIcon(WebView view, Bitmap icon) {
			if (originalWebChromeClient != null) {
				originalWebChromeClient.onReceivedIcon(view, icon);
			} else {
				super.onReceivedIcon(view, icon);
			}
		}

		@Override
		public void onReceivedTitle(WebView view, String title) {
			if (originalWebChromeClient != null) {
				originalWebChromeClient.onReceivedTitle(view, title);
			} else {
				super.onReceivedTitle(view, title);
			}
		}

		@Override
		public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed) {
			if (originalWebChromeClient != null) {
				originalWebChromeClient.onReceivedTouchIconUrl(view, url, precomposed);
			} else {
				super.onReceivedTouchIconUrl(view, url, precomposed);
			}
		}

		@Override
		public void onRequestFocus(WebView view) {
			if (originalWebChromeClient != null) {
				originalWebChromeClient.onRequestFocus(view);
			} else {
				super.onRequestFocus(view);
			}
		}

		@Override
		public void onShowCustomView(View view, CustomViewCallback callback) {
			if (originalWebChromeClient != null) {
				originalWebChromeClient.onShowCustomView(view, callback);
			} else {
				super.onShowCustomView(view, callback);
			}
		}

		public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
			if (Build.VERSION.SDK_INT >= 14) {
				if (originalWebChromeClient != null) {
					originalWebChromeClient.onShowCustomView(view, requestedOrientation, callback);
				} else {
					super.onShowCustomView(view, requestedOrientation, callback);
				}
			}
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
		private boolean isFinished = false;
		private ViewRecorder viewRecorder = null;

		public final static boolean DEBUG = false;

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
				time = Long.valueOf(jsonObject.getString("time"));
				familyString = jsonObject.getString("familyString");
				x = Integer.valueOf(jsonObject.getString("left"));
				y = Integer.valueOf(jsonObject.getString("top"));
				width = Integer.valueOf(jsonObject.getString("width"));
				height = Integer.valueOf(jsonObject.getString("height"));
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
			if (DEBUG) {
				System.out.println("[action:" + action + "] [familyString:" + familyString + "] [locationX:"
						+ locationX + "] [locationY:" + locationY + "] [time:" + time + "] [tag:" + tag + "]");
			}
			WebElementRecordEvent event = new WebElementRecordEvent(webView, familyString, action, locationX,
					locationY, time, value, tag);
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
		public View view;
		public String familyString;
		public String action;
		public int x;
		public int y;
		public long time;
		public String value;
		public String tag;

		public WebElementRecordEvent(View view, String familyString, String action, int x, int y, long time,
				String value, String tag) {
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
