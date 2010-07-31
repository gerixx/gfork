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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.swing.JFrame;

import org.gfork.Fork;
import org.junit.Before;
import org.junit.Test;

public class JFrameTaskTest {

	@Before
	public void init() {
		// configure logging before every test because some of the tests disable it
		Fork.setLoggingEnabled(true);
		Fork.setJvmOptionsForAll(new String[] {
				"-Djava.util.logging.config.file=./logging_forkRunner.properties",
		});
	}

	@Test
	public void testJFrameTask1() throws InterruptedException {
		JFrame frame = new JFrame("JFork - Sample - Swing - Frame 1");
		frame.setSize(400, 300);
		frame.setLocation(300, 300);
		
		final JFrameTaskT task = new JFrameTaskT(frame);
		frame.setVisible(true);
		Thread taskRun = new Thread() {
			public void run() {
				try {
					task.run();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		taskRun.start();
		Thread.sleep(500);
		task.shutdown();
	}
	
	@Test
	public void testJFrameTask2() throws Exception {
		String title = "JFork - Sample - Swing - Frame 1";
		JFrame f1 = new JFrame(title);
		f1.setSize(400, 300);
		f1.setLocation(300, 300);
		
		JFrameTask f1t = new JFrameTaskT(f1);
		JFrameFork fork1 = new JFrameFork(f1t);
		
		assertEquals(title, fork1.getTitle());
		fork1.setVisible(true);
		assertTrue(fork1.isVisible());
		Thread.sleep(200);
		fork1.setVisible(false);
		assertFalse(fork1.isVisible());
		fork1.shutdown();
	}
}
