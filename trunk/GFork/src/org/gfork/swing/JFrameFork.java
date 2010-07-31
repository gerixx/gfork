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

package org.gfork.swing;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;

import javax.swing.JFrame;

import org.gfork.ForkCallable;

/**
 * Creates a new process and opens the {@link JFrame} window defined
 * by the given frame task, see {@link JFrameTask}.
 * 
 * @author Gerald Ehmayer
 *
 */
public class JFrameFork {

	protected final ForkCallable<JFrameTask> fork;
	protected final JFrameTask frameTask;
	
	public JFrameFork(final JFrameTask frameTask) throws Exception {
		this.frameTask = frameTask;
		this.fork = new ForkCallable<JFrameTask>(frameTask);
		fork.execute();
	}
	
	/**
	 * Remote call: set frame to visible or hides it
	 * 
	 * @param visible
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws IOException 
	 */
	public void setVisible(final boolean visible) throws SecurityException, NoSuchMethodException, IOException {
		final Method method = frameTask.getClass().getMethod("setVisible", boolean.class);
		this.fork.call(method, visible);
	}
	
	/**
	 * Remote call: retrieves if frame is visible.
	 * 
	 * @return true if frame is visible
	 * @throws IOException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws ClassNotFoundException 
	 */
	public boolean isVisible() throws IOException, SecurityException, NoSuchMethodException, ClassNotFoundException {
		final Method method = frameTask.getClass().getMethod("isVisible");
		return this.fork.call(boolean.class, method);
	}
	
	/**
	 * Remote call: retrieves title of the frame.
	 * 
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public String getTitle() throws SecurityException, NoSuchMethodException, IOException, ClassNotFoundException {
		final Method method = frameTask.getClass().getMethod("getTitle");
		return this.fork.call(String.class, method);
	}

	/**
	 * Calls any remote public method of the linkable task object.
	 * 
	 * @param method
	 * @param args
	 * @throws IOException 
	 */
	public void call(final Method method, final Serializable... args) throws IOException {
		fork.call(method, args);
	}
	
	/**
	 * Calls any remote public method of the linkable task object.
	 * 
	 * @param <R>
	 * @param returnValue dummy to define the return type, must not be null
	 * @param method
	 * @param args
	 * @return
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public <R extends Serializable> R call(final Class<R> typeReturnValue, final Method method, final Serializable... args) throws IOException, ClassNotFoundException {
		return fork.call(typeReturnValue, method, args);
	}

	/**
	 * Closes the frame and ends the task fork main thread.
	 * 
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws IOException 
	 */
	public void shutdown() throws SecurityException, NoSuchMethodException, IOException {
		this.fork.shutdown();
	}

	public String getStdErr() throws InterruptedException, IllegalAccessException {
		return fork.getStdErr();
	}

	public boolean isFinished() {
		return fork.isFinished();
	}
}
