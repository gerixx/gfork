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
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gfork.types.MethodArgumentsException;

/**
 * Use {@link ForkLink} objects to enable tasks to communicate with linked fork processes.
 * Its task have to implement interface {@link Linkable}. 
 * {@link Linkable} tasks can receive I/O streams connecting
 * to its predecessor and successor fork process.
 * 
 * @author Gerald Ehmayer
 *
 * @param <TASK_TYPE> the task type, template upper bound type is {@link Linkable}
 * @param <RETURN_TYPE> the return value type of the task
 */
public class ForkLink<TASK_TYPE extends Linkable, RETURN_TYPE extends Serializable> extends Fork<TASK_TYPE, RETURN_TYPE> {

	private static final long serialVersionUID = 1L;

	protected static final String JOB_PROCESS_NOT_EXECUTING = "Task process was not started yet.";
	
	protected Integer successorPort;
	
	public <T extends Serializable & Runnable> ForkLink(final T task) throws IOException, SecurityException,
			NoSuchMethodException, MethodArgumentsException {
		super(task);
	}
	
	public ForkLink(final TASK_TYPE task, final Method method, final Serializable... args) throws IOException, MethodArgumentsException {
		super(task, method, args);
	}
	
	
	/**
	 * For internal use only: defines the port of the successor task to where this Fork 
	 * should connect to for input/output.
	 * 
	 * @param port TCP port of the successor task process where this Fork is linked to
	 */
	void setSuccessorPort(final int port) {
		successorPort = port;
	}
	
	/**
	 * Retrieves IO port from task link process.
	 * 
	 * @return IO port for communication
	 * @throws NumberFormatException
	 * @throws IOException
	 * @throws IllegalAccessException
	 * @throws InterruptedException
	 */
	public int readForkListenerPort() throws NumberFormatException, IOException, IllegalAccessException, InterruptedException {
		int cnt = 0;
		while (getStatusInfo() == null) {
			Thread.sleep(200);
			if (cnt > 30) {
				throw new RuntimeException("Reading fork listener port timed out.");
			}
			cnt++;
		}
		if (! getStatusInfo().startsWith("port:")) {
			throw new RuntimeException("no input port available");
		}
		return Integer.parseInt(getStatusInfo().substring(5));
	}
	
	@Override
	protected String[] createCmdArray() throws IOException {
		if (successorPort == null) {
			return super.createCmdArray();
		}
		final List<String> argsList = Arrays.asList(super.createCmdArray());
		final ArrayList<String> vmArgs = new ArrayList<String>(argsList);
		vmArgs.add(successorPort.toString());
		return (String[]) vmArgs.toArray(new String[vmArgs.size()]);
	}

	
}
