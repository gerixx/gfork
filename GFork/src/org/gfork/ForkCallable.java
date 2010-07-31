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

import org.gfork.types.MethodArgumentsException;

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
	private TASK_TYPE task;

	@SuppressWarnings("unchecked")
	public <T extends Serializable & Runnable> ForkCallable(T task) throws IOException, SecurityException,
			NoSuchMethodException, MethodArgumentsException {
		super(task);
		this.task = (TASK_TYPE) task;
	}

	/**
	 * Calls any remote public method of the linkable task object.
	 * 
	 * @param method method to be called which has no return value
	 * @param args arguments of the method
	 * @throws IOException 
	 */
	public final void call(final Method method, final Serializable... args) throws IOException {
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
	public final <T extends Serializable> T call(Class<T> typeReturnValue, final Method method,
			final Serializable... args) throws IOException, ClassNotFoundException {
		synchronized (oout) {
			call(method, args);
			return (T) getOin().readObject();
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
		final Method method = task.getClass().getMethod("shutdown");
		this.call(method);
	}

}
