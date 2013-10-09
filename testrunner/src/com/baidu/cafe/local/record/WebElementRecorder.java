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
import java.util.HashMap;
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
			outputEvent.setCode(String.format(
					"local.recordReplay.clickOnWebElementByFamilyString(\"%s\"); // Click On [%s]", e.familyString,
					e.tag));
			viewRecorder.offerOutputEventQueue(outputEvent);
		}
		return eventOffered;
	}

	/**
	 * @param webView
	 * @return
	 */
	private WebViewClient getOriginalWebViewClient(final WebView webView) {
		// save old WebViewClient
		WebViewClient mWebViewClient = null;
		try {
			// print("Build.VERSION.SDK_INT : " + Build.VERSION.SDK_INT);
			if (Build.VERSION.SDK_INT > 14) {
				Object originalWebViewClassic = viewRecorder.getLocalLib().invoke(webView, "android.webkit.WebView",
						"getWebViewProvider", new Class[] {}, new Object[] {});
				Object originalCallbackProxy = viewRecorder.getLocalLib().getField(originalWebViewClassic, null,
						"mCallbackProxy");
				mWebViewClient = (WebViewClient) viewRecorder.getLocalLib().getField(originalCallbackProxy, null,
						"mWebViewClient");
			} else {
				print("getClass:" + webView.getClass());
				mWebViewClient = (WebViewClient) viewRecorder.getLocalLib().invoke(webView, "android.webkit.WebView",
						"getWebViewClient", new Class[] {}, new Object[] {});
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

	/**
	 * use java reflect mechanism to get original WebChromeClient object
	 * 
	 * @param webView
	 * @return
	 */
	private WebChromeClient getOriginalWebChromeClient(final WebView webView) {
		WebChromeClient mWebChromeClient = null;
		try {
			if (Build.VERSION.SDK_INT > 14) {
				Object originalWebViewClassic = viewRecorder.getLocalLib().invoke(webView, "android.webkit.WebView",
						"getWebViewProvider", new Class[] {}, new Object[] {});
				Object originalCallbackProxy = viewRecorder.getLocalLib().getField(originalWebViewClassic, null,
						"mCallbackProxy");
				mWebChromeClient = (WebChromeClient) viewRecorder.getLocalLib().getField(originalCallbackProxy, null,
						"mWebChromeClient");
			} else {
				mWebChromeClient = (WebChromeClient) viewRecorder.getLocalLib().invoke(webView,
						"android.webkit.WebView", "getWebChromeClient", new Class[] {}, new Object[] {});
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

	public void setHookedWebChromeClient(final WebView webView) {
		webElementEventCreator.prepareForStart();
		if (webView != null) {
			webView.post(new Runnable() {
				public void run() {
					webView.getSettings().setJavaScriptEnabled(true);
					final WebChromeClient originalWebChromeClient = getOriginalWebChromeClient(webView);
					if (originalWebChromeClient != null) {
						webView.setWebChromeClient(new WebChromeClient() {
							HashMap<String, Boolean> invoke = new HashMap<String, Boolean>();

							/**
							 * Overrides onJsPrompt in order to create
							 * {@code WebElement} objects based on the web
							 * elements attributes prompted by the injections of
							 * JavaScript
							 */

							@Override
							public boolean onJsPrompt(WebView view, String url, String message, String defaultValue,
									JsPromptResult r) {

								if (message != null) {
									if (message.endsWith("WebElementRecorder-finished")) {
										// Log.i("onJsPrompt : " + message);
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
								Bitmap ret = null;
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									ret = originalWebChromeClient.getDefaultVideoPoster();
									invoke.put(funcName, false);
								}
								return ret;
							}

							@Override
							public View getVideoLoadingProgressView() {
								View ret = null;
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									ret = originalWebChromeClient.getVideoLoadingProgressView();
									invoke.put(funcName, false);
								}
								return ret;
							}

							@Override
							public void getVisitedHistory(ValueCallback<String[]> callback) {
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									originalWebChromeClient.getVisitedHistory(callback);
									invoke.put(funcName, false);
								}
							}

							@Override
							public void onCloseWindow(WebView window) {
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									originalWebChromeClient.onCloseWindow(window);
									invoke.put(funcName, false);
								}
							}

							@Override
							public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
								boolean ret = false;
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									ret = originalWebChromeClient.onConsoleMessage(consoleMessage);
									invoke.put(funcName, false);
								}
								return ret;
							}

							@Override
							public void onConsoleMessage(String message, int lineNumber, String sourceID) {
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									originalWebChromeClient.onConsoleMessage(message, lineNumber, sourceID);
									invoke.put(funcName, false);
								}
							}

							@Override
							public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture,
									Message resultMsg) {
								boolean ret = false;
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									ret = originalWebChromeClient.onCreateWindow(view, isDialog, isUserGesture,
											resultMsg);
									invoke.put(funcName, false);
								}
								return ret;
							}

							@Override
							public void onExceededDatabaseQuota(String url, String databaseIdentifier, long quota,
									long estimatedDatabaseSize, long totalQuota, QuotaUpdater quotaUpdater) {
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									originalWebChromeClient.onExceededDatabaseQuota(url, databaseIdentifier, quota,
											estimatedDatabaseSize, totalQuota, quotaUpdater);
									invoke.put(funcName, false);
								}
							}

							@Override
							public void onGeolocationPermissionsHidePrompt() {
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									originalWebChromeClient.onGeolocationPermissionsHidePrompt();
									invoke.put(funcName, false);
								}
							}

							@Override
							public void onGeolocationPermissionsShowPrompt(String origin, Callback callback) {
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									originalWebChromeClient.onGeolocationPermissionsShowPrompt(origin, callback);
									invoke.put(funcName, false);
								}
							}

							@Override
							public void onHideCustomView() {
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									originalWebChromeClient.onHideCustomView();
									invoke.put(funcName, false);
								}
							}

							@Override
							public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
								boolean ret = false;
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									ret = originalWebChromeClient.onJsAlert(view, url, message, result);
									invoke.put(funcName, false);
								}
								return ret;
							}

							@Override
							public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
								boolean ret = false;
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									ret = originalWebChromeClient.onJsBeforeUnload(view, url, message, result);
									invoke.put(funcName, false);
								}
								return ret;
							}

							@Override
							public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
								boolean ret = false;
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									ret = originalWebChromeClient.onJsConfirm(view, url, message, result);
									invoke.put(funcName, false);
								}
								return ret;
							}

							@Override
							public boolean onJsTimeout() {
								boolean ret = false;
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									ret = originalWebChromeClient.onJsTimeout();
									invoke.put(funcName, false);
								}
								return ret;
							}

							@Override
							public void onProgressChanged(WebView view, int newProgress) {
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									originalWebChromeClient.onProgressChanged(view, newProgress);
									invoke.put(funcName, false);
								}
							}

							@Override
							public void onReachedMaxAppCacheSize(long requiredStorage, long quota,
									QuotaUpdater quotaUpdater) {
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									originalWebChromeClient.onReachedMaxAppCacheSize(requiredStorage, quota,
											quotaUpdater);
									invoke.put(funcName, false);
								}
							}

							@Override
							public void onReceivedIcon(WebView view, Bitmap icon) {
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									originalWebChromeClient.onReceivedIcon(view, icon);
									invoke.put(funcName, false);
								}
							}

							@Override
							public void onReceivedTitle(WebView view, String title) {
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									originalWebChromeClient.onReceivedTitle(view, title);
									invoke.put(funcName, false);
								}
							}

							@Override
							public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed) {
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									originalWebChromeClient.onReceivedTouchIconUrl(view, url, precomposed);
									invoke.put(funcName, false);
								}

							}

							@Override
							public void onRequestFocus(WebView view) {
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									originalWebChromeClient.onRequestFocus(view);
									invoke.put(funcName, false);
								}
							}

							@Override
							public void onShowCustomView(View view, CustomViewCallback callback) {
								String funcName = new Throwable().getStackTrace()[1].getMethodName();
								if (invoke.get(funcName) == null || !invoke.get(funcName)) {
									invoke.put(funcName, true);
									originalWebChromeClient.onShowCustomView(view, callback);
									invoke.put(funcName, false);
								}
							}

							public void onShowCustomView(View view, int requestedOrientation,
									CustomViewCallback callback) {
								if (Build.VERSION.SDK_INT >= 14) {
									String funcName = new Throwable().getStackTrace()[1].getMethodName();
									if (invoke.get(funcName) == null || !invoke.get(funcName)) {
										invoke.put(funcName, true);
										originalWebChromeClient.onShowCustomView(view, requestedOrientation, callback);
										invoke.put(funcName, false);
									}
								}
							}
						});
					} else {
						webView.setWebChromeClient(new WebChromeClient() {

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#getDefaultVideoPoster()
							 */
							@Override
							public Bitmap getDefaultVideoPoster() {
								// TODO Auto-generated method stub
								return super.getDefaultVideoPoster();
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#getVideoLoadingProgressView()
							 */
							@Override
							public View getVideoLoadingProgressView() {
								// TODO Auto-generated method stub
								return super.getVideoLoadingProgressView();
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#getVisitedHistory(android.webkit.ValueCallback)
							 */
							@Override
							public void getVisitedHistory(
									ValueCallback<String[]> callback) {
								// TODO Auto-generated method stub
								super.getVisitedHistory(callback);
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onCloseWindow(android.webkit.WebView)
							 */
							@Override
							public void onCloseWindow(WebView window) {
								// TODO Auto-generated method stub
								super.onCloseWindow(window);
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onConsoleMessage(android.webkit.ConsoleMessage)
							 */
							@Override
							public boolean onConsoleMessage(
									ConsoleMessage consoleMessage) {
								// TODO Auto-generated method stub
								return super.onConsoleMessage(consoleMessage);
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onConsoleMessage(java.lang.String, int, java.lang.String)
							 */
							@Override
							public void onConsoleMessage(String message,
									int lineNumber, String sourceID) {
								// TODO Auto-generated method stub
								super.onConsoleMessage(message, lineNumber, sourceID);
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onCreateWindow(android.webkit.WebView, boolean, boolean, android.os.Message)
							 */
							@Override
							public boolean onCreateWindow(WebView view,
									boolean isDialog, boolean isUserGesture,
									Message resultMsg) {
								// TODO Auto-generated method stub
								return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onExceededDatabaseQuota(java.lang.String, java.lang.String, long, long, long, android.webkit.WebStorage.QuotaUpdater)
							 */
							@Override
							public void onExceededDatabaseQuota(String url,
									String databaseIdentifier, long quota,
									long estimatedDatabaseSize,
									long totalQuota, QuotaUpdater quotaUpdater) {
								// TODO Auto-generated method stub
								super.onExceededDatabaseQuota(url, databaseIdentifier, quota,
										estimatedDatabaseSize, totalQuota, quotaUpdater);
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onGeolocationPermissionsHidePrompt()
							 */
							@Override
							public void onGeolocationPermissionsHidePrompt() {
								// TODO Auto-generated method stub
								super.onGeolocationPermissionsHidePrompt();
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onGeolocationPermissionsShowPrompt(java.lang.String, android.webkit.GeolocationPermissions.Callback)
							 */
							@Override
							public void onGeolocationPermissionsShowPrompt(
									String origin, Callback callback) {
								// TODO Auto-generated method stub
								super.onGeolocationPermissionsShowPrompt(origin, callback);
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onHideCustomView()
							 */
							@Override
							public void onHideCustomView() {
								// TODO Auto-generated method stub
								super.onHideCustomView();
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onJsAlert(android.webkit.WebView, java.lang.String, java.lang.String, android.webkit.JsResult)
							 */
							@Override
							public boolean onJsAlert(WebView view, String url,
									String message, JsResult result) {
								// TODO Auto-generated method stub
								return super.onJsAlert(view, url, message, result);
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onJsBeforeUnload(android.webkit.WebView, java.lang.String, java.lang.String, android.webkit.JsResult)
							 */
							@Override
							public boolean onJsBeforeUnload(WebView view,
									String url, String message, JsResult result) {
								// TODO Auto-generated method stub
								return super.onJsBeforeUnload(view, url, message, result);
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onJsConfirm(android.webkit.WebView, java.lang.String, java.lang.String, android.webkit.JsResult)
							 */
							@Override
							public boolean onJsConfirm(WebView view,
									String url, String message, JsResult result) {
								// TODO Auto-generated method stub
								return super.onJsConfirm(view, url, message, result);
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onJsPrompt(android.webkit.WebView, java.lang.String, java.lang.String, java.lang.String, android.webkit.JsPromptResult)
							 */
							@Override
							public boolean onJsPrompt(WebView view, String url,
									String message, String defaultValue,
									JsPromptResult result) {
								if (message != null) {
									if (message.endsWith("WebElementRecorder-finished")) {
										// Log.i("onJsPrompt : " + message);
										webElementEventCreator.setFinished(true);
									} else {
										webElementEventCreator.createWebElementEvent(message, view);
									}
								}
								result.confirm();
								return true;
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onJsTimeout()
							 */
							@Override
							public boolean onJsTimeout() {
								// TODO Auto-generated method stub
								return super.onJsTimeout();
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onProgressChanged(android.webkit.WebView, int)
							 */
							@Override
							public void onProgressChanged(WebView view,
									int newProgress) {
								// TODO Auto-generated method stub
								super.onProgressChanged(view, newProgress);
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onReachedMaxAppCacheSize(long, long, android.webkit.WebStorage.QuotaUpdater)
							 */
							@Override
							public void onReachedMaxAppCacheSize(
									long requiredStorage, long quota,
									QuotaUpdater quotaUpdater) {
								// TODO Auto-generated method stub
								super.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onReceivedIcon(android.webkit.WebView, android.graphics.Bitmap)
							 */
							@Override
							public void onReceivedIcon(WebView view, Bitmap icon) {
								// TODO Auto-generated method stub
								super.onReceivedIcon(view, icon);
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onReceivedTitle(android.webkit.WebView, java.lang.String)
							 */
							@Override
							public void onReceivedTitle(WebView view,
									String title) {
								// TODO Auto-generated method stub
								super.onReceivedTitle(view, title);
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onReceivedTouchIconUrl(android.webkit.WebView, java.lang.String, boolean)
							 */
							@Override
							public void onReceivedTouchIconUrl(WebView view,
									String url, boolean precomposed) {
								// TODO Auto-generated method stub
								super.onReceivedTouchIconUrl(view, url, precomposed);
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onRequestFocus(android.webkit.WebView)
							 */
							@Override
							public void onRequestFocus(WebView view) {
								// TODO Auto-generated method stub
								super.onRequestFocus(view);
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onShowCustomView(android.view.View, android.webkit.WebChromeClient.CustomViewCallback)
							 */
							@Override
							public void onShowCustomView(View view,
									CustomViewCallback callback) {
								// TODO Auto-generated method stub
								super.onShowCustomView(view, callback);
							}

							/* (non-Javadoc)
							 * @see android.webkit.WebChromeClient#onShowCustomView(android.view.View, int, android.webkit.WebChromeClient.CustomViewCallback)
							 */
							@Override
							public void onShowCustomView(View view,
									int requestedOrientation,
									CustomViewCallback callback) {
								// TODO Auto-generated method stub
								super.onShowCustomView(view, requestedOrientation, callback);
							}
							
						});
					}
				}
			});
		}
	}

	public void setHookedWebViewClient(final WebView webView, final String javaScript) {
		webView.post(new Runnable() {
			// @Override
			public void run() {
				final WebViewClient orginalWebViewClient = getOriginalWebViewClient(webView);
				if (orginalWebViewClient != null) {
					webView.setWebViewClient(new WebViewClient() {

						HashMap<String, Boolean> invoke = new HashMap<String, Boolean>();

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
							String funcName = new Throwable().getStackTrace()[1].getMethodName();
							if (invoke.get(funcName) == null || !invoke.get(funcName)) {
								invoke.put(funcName, true);
								orginalWebViewClient.onLoadResource(view, url);
								invoke.put(funcName, false);
							}
						}

						@Override
						public void onPageFinished(WebView view, String url) {
							String funcName = new Throwable().getStackTrace()[1].getMethodName();
							if (invoke.get(funcName) == null || !invoke.get(funcName)) {
								invoke.put(funcName, true);
								orginalWebViewClient.onPageFinished(view, url);
								if (url != null) {
									hookWebElements(view, javaScript);
								}
								invoke.put(funcName, false);
							}
						}

						@Override
						public void onPageStarted(WebView view, String url, Bitmap favicon) {
							String funcName = new Throwable().getStackTrace()[1].getMethodName();
							if (invoke.get(funcName) == null || !invoke.get(funcName)) {
								invoke.put(funcName, true);
								orginalWebViewClient.onPageStarted(view, url, favicon);
								invoke.put(funcName, false);
							}
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
							boolean ret = false;
							String funcName = new Throwable().getStackTrace()[1].getMethodName();
							if (invoke.get(funcName) == null || !invoke.get(funcName)) {
								invoke.put(funcName, true);
								ret = orginalWebViewClient.shouldOverrideUrlLoading(view, url);
								invoke.put(funcName, false);
							}
							return ret;
						}

					});
				} else {
					// set hook WebViewClient
					webView.setWebViewClient(new WebViewClient() {
						
						/* (non-Javadoc)
						 * @see android.webkit.WebViewClient#doUpdateVisitedHistory(android.webkit.WebView, java.lang.String, boolean)
						 */
						@Override
						public void doUpdateVisitedHistory(WebView view,
								String url, boolean isReload) {
							// TODO Auto-generated method stub
							super.doUpdateVisitedHistory(view, url, isReload);
						}

						/* (non-Javadoc)
						 * @see android.webkit.WebViewClient#onFormResubmission(android.webkit.WebView, android.os.Message, android.os.Message)
						 */
						@Override
						public void onFormResubmission(WebView view,
								Message dontResend, Message resend) {
							// TODO Auto-generated method stub
							super.onFormResubmission(view, dontResend, resend);
						}

						/* (non-Javadoc)
						 * @see android.webkit.WebViewClient#onLoadResource(android.webkit.WebView, java.lang.String)
						 */
						@Override
						public void onLoadResource(WebView view, String url) {
							// TODO Auto-generated method stub
							super.onLoadResource(view, url);
						}

						/* (non-Javadoc)
						 * @see android.webkit.WebViewClient#onPageStarted(android.webkit.WebView, java.lang.String, android.graphics.Bitmap)
						 */
						@Override
						public void onPageStarted(WebView view, String url,
								Bitmap favicon) {
							// TODO Auto-generated method stub
							super.onPageStarted(view, url, favicon);
						}

						/* (non-Javadoc)
						 * @see android.webkit.WebViewClient#onReceivedError(android.webkit.WebView, int, java.lang.String, java.lang.String)
						 */
						@Override
						public void onReceivedError(WebView view,
								int errorCode, String description,
								String failingUrl) {
							// TODO Auto-generated method stub
							super.onReceivedError(view, errorCode, description, failingUrl);
						}

						/* (non-Javadoc)
						 * @see android.webkit.WebViewClient#onReceivedHttpAuthRequest(android.webkit.WebView, android.webkit.HttpAuthHandler, java.lang.String, java.lang.String)
						 */
						@Override
						public void onReceivedHttpAuthRequest(WebView view,
								HttpAuthHandler handler, String host,
								String realm) {
							// TODO Auto-generated method stub
							super.onReceivedHttpAuthRequest(view, handler, host, realm);
						}

						/* (non-Javadoc)
						 * @see android.webkit.WebViewClient#onReceivedLoginRequest(android.webkit.WebView, java.lang.String, java.lang.String, java.lang.String)
						 */
						@Override
						public void onReceivedLoginRequest(WebView view,
								String realm, String account, String args) {
							// TODO Auto-generated method stub
							super.onReceivedLoginRequest(view, realm, account, args);
						}

						/* (non-Javadoc)
						 * @see android.webkit.WebViewClient#onReceivedSslError(android.webkit.WebView, android.webkit.SslErrorHandler, android.net.http.SslError)
						 */
						@Override
						public void onReceivedSslError(WebView view,
								SslErrorHandler handler, SslError error) {
							// TODO Auto-generated method stub
							super.onReceivedSslError(view, handler, error);
						}

						/* (non-Javadoc)
						 * @see android.webkit.WebViewClient#onScaleChanged(android.webkit.WebView, float, float)
						 */
						@Override
						public void onScaleChanged(WebView view,
								float oldScale, float newScale) {
							// TODO Auto-generated method stub
							super.onScaleChanged(view, oldScale, newScale);
						}

						/* (non-Javadoc)
						 * @see android.webkit.WebViewClient#onTooManyRedirects(android.webkit.WebView, android.os.Message, android.os.Message)
						 */
						@Override
						public void onTooManyRedirects(WebView view,
								Message cancelMsg, Message continueMsg) {
							// TODO Auto-generated method stub
							super.onTooManyRedirects(view, cancelMsg, continueMsg);
						}

						/* (non-Javadoc)
						 * @see android.webkit.WebViewClient#onUnhandledKeyEvent(android.webkit.WebView, android.view.KeyEvent)
						 */
						@Override
						public void onUnhandledKeyEvent(WebView view,
								KeyEvent event) {
							// TODO Auto-generated method stub
							super.onUnhandledKeyEvent(view, event);
						}

						/* (non-Javadoc)
						 * @see android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView, java.lang.String)
						 */
						@Override
						public WebResourceResponse shouldInterceptRequest(
								WebView view, String url) {
							// TODO Auto-generated method stub
							return super.shouldInterceptRequest(view, url);
						}

						/* (non-Javadoc)
						 * @see android.webkit.WebViewClient#shouldOverrideKeyEvent(android.webkit.WebView, android.view.KeyEvent)
						 */
						@Override
						public boolean shouldOverrideKeyEvent(WebView view,
								KeyEvent event) {
							// TODO Auto-generated method stub
							return super.shouldOverrideKeyEvent(view, event);
						}

						/* (non-Javadoc)
						 * @see android.webkit.WebViewClient#shouldOverrideUrlLoading(android.webkit.WebView, java.lang.String)
						 */
						@Override
						public boolean shouldOverrideUrlLoading(WebView view,
								String url) {
							// TODO Auto-generated method stub
							return super.shouldOverrideUrlLoading(view, url);
						}

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

	public void hookWebView(final WebView webView) {
		setHookedWebChromeClient(webView);
		final String javaScript = getJavaScriptAsString();
		hookWebElements(webView, javaScript);
		setHookedWebViewClient(webView, javaScript);
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

		public final static boolean DEBUG = true;

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
				x = Math.round(Float.valueOf(jsonObject.getString("left")));
				y = Math.round(Float.valueOf(jsonObject.getString("top")));
				width = Math.round(Float.valueOf(jsonObject.getString("width")));
				height = Math.round(Float.valueOf(jsonObject.getString("height")));
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
