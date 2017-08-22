package com.krubo.blelibrary.utils;

import android.content.Context;
import android.util.Log;

/**
 * Log日志打印类
 */
public class LogUtil {
	/** 是否显示LOG日志，true显示 */
	public static boolean DEBUG_LOG = true;
	private static String className;
	private static String methodName;
	private static int lineNumber;

	private static String createLog(String log) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(methodName);
		buffer.append("(").append(className).append(":").append(lineNumber).append(")");
		buffer.append(log);
		return buffer.toString();
	}

	private static void getMethodNames(StackTraceElement[] sElements) {
		className = sElements[1].getFileName();
		methodName = sElements[1].getMethodName();
		lineNumber = sElements[1].getLineNumber();
	}

	public static void v(String msg) {
		if (DEBUG_LOG) {
			getMethodNames(new Throwable().getStackTrace());
			Log.v(className, createLog(msg));
		}
	}

	public static void v(Context context, int resId) {
		v(context.getApplicationContext().getString(resId));
	}

	public static void d(String msg) {
		if (DEBUG_LOG) {
			getMethodNames(new Throwable().getStackTrace());
			Log.d(className, createLog(msg));
		}
	}

	public static void d(Context context, int resId) {
		d(context.getApplicationContext().getString(resId));
	}

	public static void i(String msg) {
		if (DEBUG_LOG) {
			getMethodNames(new Throwable().getStackTrace());
			Log.i(className, createLog(msg));
		}
	}

	public static void i(Context context, int resId) {
		i(context.getApplicationContext().getString(resId));
	}

	public static void w(String msg) {
		if (DEBUG_LOG) {
			getMethodNames(new Throwable().getStackTrace());
			Log.w(className, createLog(msg));
		}
	}

	public static void w(Context context, int resId) {
		w(context.getApplicationContext().getString(resId));
	}

	public static void e(String msg) {
		if (DEBUG_LOG) {
			getMethodNames(new Throwable().getStackTrace());
			Log.e(className, createLog(msg));
		}
	}

	public static void e(Context context, int resId) {
		e(context.getApplicationContext().getString(resId));
	}

	public static void wtf(String message) {
		if (DEBUG_LOG) {
			getMethodNames(new Throwable().getStackTrace());
			Log.wtf(className, createLog(message));
		}
	}

	public static void wtf(Context context, int resId) {
		wtf(context.getApplicationContext().getString(resId));
	}

}
