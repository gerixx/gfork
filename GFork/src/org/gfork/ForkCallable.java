/*
   Copyright 2010 Gerald Ehmayer
   
   This file is part of project GFork.

    GFork is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GFork is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with GFork.  If not, see <http://www.gnu.org/licenses/>.

*/

package org.gfork;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.gfork.types.MethodArgumentsException;
import org.gfork.types.Void;

/**
 * This class provides methods <code>call</code> which can be used to call 
 * from the fork parent process to the remote task fork process. 
 * Any public method of the task object can be invoked.
 * 
 * @author Gerald Ehmayer
 *
 * @param <TASK_TYPE>
 */
public class ForkCallable<TASK_TYPE extends CallableTask> extends ForkLink<TASK_TYPE, org.gfork.types.Void> {

	private static final long serialVersionUID = 1L;
	private ObjectOutputStream oout;
	private ObjectInputStream oin; // use getter for access
	private Socket ioSocket;
	private final TASK_TYPE task;
	private AsyncCallThread asyncCallThread;

	/**
	 * Callback interface needed for asynchronous task calls.
	 * See {@link ForkCallable#callAsync(CallHandler, Method, Serializable...)}.
	 * 
	 * @author Gerald Ehmayer
	 *
	 * @param <R> return value type
	 */
	public interface CallHandler<R extends Serializable> {
		/**
		 * Implement this to handle returning asynchronous call and to receive
		 * the return value of the call. In cases of void return values it will 
		 * be invoked with return value of type {@link Void}.
		 * @param returnValue
		 */
		public void onReturn(R returnValue);
		/**
		 * Implement this to handle exceptions thrown by the asynchronous call.
		 * @param e
		 */
		public void onException(Exception e);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Serializable & Runnable> ForkCallable(final T task) throws IOException, SecurityException,
			NoSuchMethodException, MethodArgumentsException {
		super(task);
		this.task = (TASK_TYPE) task;
	}

	/**
	 * Calls any remote public void method of the linkable task object.
	 * 
	 * @param method method to be called which has no return value
	 * @param args arguments of the method
	 * @throws IOException 
	 */
	public final void call(final Method method, final Serializable... args) throws IOException {
		if (method.getReturnType() != void.class) {
			throw new IllegalArgumentException(String.format("Only method with void return type are allowed, but was '%s'", method.getReturnType().toString()));
		}
		sendCallData(method, args);
		try {
			getOin().readObject(); // dummy return value needed to block until remote execution of the call is finished
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e); // probably a bug
		} 
	}

	private void sendCallData(final Method method, final Serializable... args)
			throws IOException {
		if (!isExecuting()) {
			throw new IllegalStateException(FORK_WAS_NOT_STARTED_YET);
		}
		synchronized (oout) {
			oout.writeObject(method.getName());
			if (args != null && args.length > 0) {
				oout.writeObject(new Boolean(true));
				oout.writeObject(method.getParameterTypes());
				oout.writeObject(args);
			} else {
				oout.writeObject(new Boolean(false));
			}
			oout.flush();
		}
	}

	/**
	 * Calls any remote public method of the linkable task object.
	 * 
	 * @param typeReturnValue class of the return value type
	 * @param method method to be called
	 * @param args method arguments
	 * @return return value of the called remote method
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * 
	 */
	@SuppressWarnings("unchecked")
	public final <T extends Serializable> T call(final Class<T> typeReturnValue, final Method method,
			final Serializable... args) throws IOException, ClassNotFoundException {
		synchronized (oout) {
			sendCallData(method, args);
			return (T) getOin().readObject();
		}
	}

	/**
	 * Calls a method of the remote task of this fork process and returns immediately.
	 * With the current implementation multiple asynchronous and synchronous calls will be serialized!
	 * So this cannot be used to implement parallel execution within the remote task execution.
	 * Furthermore it is important to be aware about serialized execution when
	 * calls of synchronous and asynchronous methods are mixed for the same fork.
	 * 
	 * @param <T> return value type, use {@link Void} for void return values
	 * @param callback handler that will be invoked when the call returns
	 * @param method method to be called
	 * @param args method arguments
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InterruptedException 
	 */
	public final <T extends Serializable> void callAsync(final CallHandler<T> callback, final Class<T> typeReturnValue, final Method method,
			final Serializable... args) throws IOException, ClassNotFoundException, InterruptedException {
		synchronized (oout) {
			if (asyncCallThread == null) {
				asyncCallThread = new AsyncCallThread();
				asyncCallThread.setDaemon(true);
				asyncCallThread.start();
			}
			asyncCallThread.put(new CallInfo(typeReturnValue, callback, method, args));
		}
	}
	
	@Override
	public synchronized void execute() throws Exception {
		super.execute();
		final int ioPort = readForkListenerPort();
		ioSocket = new Socket("127.0.0.1", ioPort);
		this.oout = new ObjectOutputStream(ioSocket.getOutputStream());
	}

	/**
	 * @return object input stream to read from the task
	 * @throws IOException
	 */
	protected ObjectInputStream getOin() throws IOException {
		if (!isExecuting()) {
			throw new IllegalStateException(FORK_WAS_NOT_STARTED_YET);
		}
		if (oin == null) {
			oin = new ObjectInputStream(ioSocket.getInputStream());
		}
		return oin;
	}
	
	/**
	 * @return object output stream to wire to the task
	 * @throws IOException
	 */
	protected ObjectOutputStream getOout() throws IOException {
		if (!isExecuting()) {
			throw new IllegalStateException(FORK_WAS_NOT_STARTED_YET);
		}
		return oout;
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		ioSocket.close();
	}

	/**
	 * Signals the remote callable task to end now, see also {@link CallableTask#shutdown()}.
	 * 
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws IOException
	 */
	public void shutdown() throws SecurityException, NoSuchMethodException, IOException {
		if (!isExecuting()) {
			throw new IllegalStateException(FORK_WAS_NOT_STARTED_YET);
		}
		if (asyncCallThread != null) {
			asyncCallThread.shutdown();
		}
		final Method method = task.getClass().getMethod("shutdown");
		this.call(method);
	}

	private class CallInfo {
	
		@SuppressWarnings("rawtypes")
		private final CallHandler callback;
		private final Method method;
		private final Serializable[] args;
		@SuppressWarnings("rawtypes")
		private final Class typeReturnValue;
	
		@SuppressWarnings("rawtypes")
		public CallInfo(final Class typeReturnValue, final CallHandler callback, final Method method,
				final Serializable[] args) {
					this.typeReturnValue = typeReturnValue;
					this.callback = callback;
					this.method = method;
					this.args = args;
		}
	
		public Method getMethod() {
			return method;
		}
	
		public Serializable[] getArgs() {
			return args;
		}
		
		@SuppressWarnings("rawtypes")
		public CallHandler getCallHandler() {
			return callback;
		}
	
		@SuppressWarnings("rawtypes")
		public Class getTypeReturnValue() {
			return typeReturnValue;
		}
	}

	private class AsyncCallThread extends Thread {
		private static final int QUEUE_POLL_SECONDS = 1;
		private LinkedBlockingDeque<CallInfo> asyncCallQueue = new LinkedBlockingDeque<CallInfo>();
		private boolean stop;
		
		public AsyncCallThread() {
			setName("jforkAsyncCalls");
		}
		public synchronized void shutdown() {
			stop = true;
			asyncCallQueue.clear();
			try {
				Thread.sleep(QUEUE_POLL_SECONDS + 100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				stdErrText.append(String.format("ERROR %s: %s%n", getName(), e.toString()));
			}
		}
		public synchronized void put(CallInfo callInfo) throws InterruptedException {
			asyncCallQueue.put(callInfo);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			CallInfo callInfo = null;
			try {
				while(!isFinished() && !stop) {
					callInfo = asyncCallQueue.poll(QUEUE_POLL_SECONDS, TimeUnit.SECONDS);
					if (callInfo != null) {
						synchronized (oout) {
							if (callInfo.getTypeReturnValue() != Void.class) {
								final Serializable result = call(callInfo.getTypeReturnValue(), callInfo.getMethod(), callInfo.getArgs());
								callInfo.getCallHandler().onReturn(result);
							} else {
								call(callInfo.getMethod(), callInfo.getArgs());
								callInfo.getCallHandler().onReturn(new Void());
							}
						}
					}
				}
			} catch (final Exception e) {
				e.printStackTrace();
				if (callInfo != null && callInfo.getCallHandler() != null) {
					callInfo.getCallHandler().onException(e);
				}
				stdErrText.append(String.format("ERROR %s: %s%n", getName(), e.toString()));
			}
		}
	}
}
