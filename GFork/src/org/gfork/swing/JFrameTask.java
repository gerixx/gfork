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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JFrame;

import org.gfork.CallableTask;

/**
 * Provides a task to run a {@link JFrame} in a single subprocess.
 * Derive from this class and override {@link JFrameTask#initialize(JFrame)}
 * to customize the frame content and its behavior. As {@link JFrameTask} is
 * derives from {@link CallableTask} any of its public methods can be called from 
 * the parent process.
 * 
 * @author Gerald Ehmayer
 *
 */
public abstract class JFrameTask extends CallableTask {

	private transient final static Logger log = Logger.getLogger(JFrameTask.class.getName());

	private static final long serialVersionUID = 1L;
	
	protected JFrame frame;
	
	public JFrameTask(final JFrame frame) {
		if (frame == null) {
			throw new IllegalArgumentException("Null not allowed for parameter frame");
		}
		this.frame = frame;
	}
	
	public void setVisible(boolean visible) {
		frame.setVisible(visible);
	}
	
	public boolean isVisible() {
		return frame.isVisible();
	}
	
	public String getTitle() {
		return frame.getTitle();
	}

	public String getName() {
		return frame.getName();
	}	

	/**
	 * Initializes the task on fork startup. Adds a window close listener
	 * to its JFrame per default. Override this to customize the 
	 * frame contents on task startup.
	 * 
	 * @param frame
	 * @throws IOException
	 */
	@Override
	protected void initialize() throws IOException {
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				shutdown();
			}
		});
		log.info("frame initialized");
	}

	/**
	 * Overridden in order to hide the frame.
	 */
	@Override
	public void shutdown() {
		frame.setVisible(false);
		frame = null; // not needed to be back propagated to parent process
		log.info("frame destroyed");
		super.shutdown();
	}
	
}
