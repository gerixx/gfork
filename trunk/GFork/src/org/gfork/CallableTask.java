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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gfork.helpers.LinkableAdapter;
import org.gfork.sample.swing.DukePaintTask;

/**
 * This abstract task class implements a listener
 * that awaits calls from the parent process. It forwards incoming
 * calls to the derived class using Java Reflection. Use {@link ForkCallable}
 * to create a callable task fork.
 * 
 * For a detailed example see {@link DukePaintTask}.
 * 
 * @author Gerald Ehmayer
 *
 */
public abstract class CallableTask extends LinkableAdapter implements Runnable {

	private static final long serialVersionUID = 1L;

	private transient final static Logger log = Logger.getLogger(CallableTask.class.getName());

	// hide i/o streams, exclusively used for dynamic method calls
	private transient InputStream predReadStream;
	private transient OutputStream predWriteStream;
	
	protected transient ObjectInputStream oin;
	protected transient ObjectOutputStream oout;
	private transient boolean stop;

	private transient boolean ready;
	private transient Object lock;

	/**
	 * Creates and starts the call listener.
	 */
	@Override
	public final void init() throws IOException {
		super.init();
		this.predReadStream = super.predReadStream;
		this.predWriteStream = super.predWriteStream;
		
		oin = new ObjectInputStream(this.predReadStream);
		oout = new ObjectOutputStream(this.predWriteStream);
		
		final Thread reader = new Thread("jforkCallableTaskListener") {
			@Override
			public void run() {
				log.info("call listener starting...");
				try {
					while (!isReadyToBeCalled()) {
						Thread.sleep(300);
					}
					while (!stop) {
						log.fine("call listener waits for call...");
						final String methodName = (String) oin.readObject();
						final Boolean withArgs = (Boolean) oin.readObject();
						final Object retVal;
						final Method method;
						final Object target = getImplementingObject();
						if (withArgs) {
							final Class<?>[] types = (Class<?>[]) oin.readObject();
							final Object[] values = (Object[]) oin.readObject();
							log.fine(String.format("call '%s' of class %s with %d argument(s)", methodName, target.getClass(), values.length));
							method = target.getClass().getMethod(methodName, types);
							retVal = method.invoke(target, values);
						} else {
							log.fine(String.format("call '%s' of class %s with no arguments", methodName, target.getClass()));
							method = target.getClass().getMethod(methodName);
							retVal = method.invoke(target);
						}
						log.fine(String.format("return value '%s' of type %s", retVal, method.getReturnType()));
						if (method.getReturnType() != void.class) {
							oout.writeObject(retVal);
						} else {
							oout.writeObject("void");
						}
						oout.flush();
					}
				} catch (final SocketException e) {
					log.log(Level.WARNING, "call listener socket", e);
					shutdown();
				} catch (final Exception e) {
					log.log(Level.SEVERE, "call listener error", e);
					shutdown();
				}
				log.info("call listener stopped");
			}
			
		};
		reader.start();
	}

	/**
	 * Stops the call listener and the task and ends the fork process.
	 */
	public void shutdown() {
		log.info("callable shutdown");
		synchronized (lock) {
			if (! isStop()) {
				setStop(true);
				lock.notifyAll();
			}
		}
	}
	
	public abstract Object getImplementingObject();
	
	/**
	 * Indicates if a {@link CallableTask} is ready to be called from remote.
	 * Incoming calls are blocked until this method returns true.
	 * 
	 * @return true when ready for receiving calls
	 */
	protected boolean isReadyToBeCalled() {
		return ready;
	}
	
	/**
	 * Indicates if incoming call listener should stop.
	 * 
	 * @return true if call listener should stop
	 */
	protected boolean isStop() {
		return stop;
	}
	
	/**
	 * Stops the incoming call listener.
	 * The task ends if no more non-daemon threads are running.
	 */
	protected void setStop(boolean stop) {
		this.stop = stop;
	}
	
	/**
	 * Invokes xxx and waits until task is stopped.
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public final void run() {
		if (lock != null) {
			throw new IllegalStateException("Method run is already executing.");
		}
		lock = new Object();
		synchronized (lock) {
			log.info("callable initializing...");
			try {
				initialize();
				log.info("callable initialized");
				ready = true;
				while (!isStop()) {
					lock.wait();
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			log.info("callable stopped");
		}
	}

	protected abstract void initialize() throws Exception;
	
}
