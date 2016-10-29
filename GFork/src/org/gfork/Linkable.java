package org.gfork;
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


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.gfork.internal.run.ForkRunner;



/**
 * Defines interface needed to implement linked tasks, see {@link ForkLink}.
 * Linked tasks are enabled for communication, means, every task 
 * can read/write from/to its predecessor process and 
 * read/write from/to its successor process (like pipes).
 * Predecessor and successor of a linked task are either linkable tasks too
 * or the parent process that created the linked task.
 * 
 * @see ForkLink
 * 
 * @author Gerald Ehmayer
 *
 */
public interface Linkable extends Serializable {
	
	/**
	 * Set the data input for a task - data from the predecessor task. 
	 * @throws IOException 
	 */
	public void setPredTaskReadStream(InputStream in) throws IOException;

	public void setPredTaskWriteStream(OutputStream out) throws IOException;
	
	/**
	 * Set the stream a task use to write output - data for the successor task.
	 * @throws IOException 
	 */
	public void setSuccTaskWriteStream(OutputStream out) throws IOException;
	
	public void setSuccTaskReadStream(InputStream in) throws IOException;
	
	/**
	 * {@link ForkRunner} invokes this method on creation time when the
	 * fork process is starting.
	 * @param loggingEnabled 
	 * 
	 * @throws IOException
	 */
	public void init(boolean loggingEnabled) throws IOException;
}
