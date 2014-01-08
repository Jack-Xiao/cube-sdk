package com.srain.cube.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

import android.os.Handler;
import android.os.Message;

/**
 * A class which encapsulate a task that can excute in background thread and can be cancelled.
 * 
 * @author huqiu.lhq
 */
public abstract class SimpleTask implements Runnable {

	private static final int STATE_NEW = 0x0001;
	private static final int STATE_RUNNING = 0x0010;
	private static final int STATE_COMPLETING = 0x0100;
	private static final int STATE_CANCELLED = 0x0101;

	private static final int TASK_DONE = 0x01;
	private static InternalHandler sHandler = new InternalHandler();
	private Thread mCurrenThread;
	private AtomicInteger mState = new AtomicInteger(STATE_NEW);

	/**
	 * A woker will excute this method in a background thead
	 */
	public abstract void doInBackground();

	/**
	 * will be called after doInBackground();
	 */
	public abstract void onFinish();

	/**
	 * When the Task is Cancelled.
	 */
	protected void onCancel() {
	}

	@Override
	public void run() {
		if (!mState.compareAndSet(STATE_NEW, STATE_RUNNING)) {
			return;
		}
		mCurrenThread = Thread.currentThread();
		doInBackground();
		sHandler.obtainMessage(TASK_DONE, this).sendToTarget();
	}

	/**
	 * check whether this work is canceled.
	 */
	public boolean isCancelled() {
		return mState.get() == STATE_CANCELLED;
	}

	public boolean isDone() {
		return mState.get() == STATE_COMPLETING;
	}

	public void cancel(boolean mayInterruptIfRunning) {
		if (mState.get() >= STATE_COMPLETING) {
			return;
		} else {
			if (mState.get() == STATE_RUNNING && mayInterruptIfRunning && null != mCurrenThread) {
				try {
					mCurrenThread.interrupt();
				} catch (Exception e) {
				}
			}
			mState.set(STATE_CANCELLED);
			onCancel();
		}
	}

	private static class InternalHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			SimpleTask work = (SimpleTask) msg.obj;
			switch (msg.what) {
			case TASK_DONE:
				work.mState.set(STATE_COMPLETING);
				work.onFinish();
				break;
			default:
				break;
			}
		}
	}
}