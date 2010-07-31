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

package org.gfork.sample.swing;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Random;

import javax.swing.JFrame;

import org.gfork.Fork;
import org.gfork.swing.JFrameFork;

/**
 * Opens 4 windows, every frame is running in a single subprocess.
 * Frame tasks are implemented by {@link DukePaintTask} and paint the duke image 
 * onto random positions. Timing and positions are controlled by the main process.
 * 
 * @author Gerald Ehmayer
 *
 */
public class FrameDemo {

	public static void main(String[] args) {
		File logFile = new File("./tmp/swingSamples.log");
		if (logFile.exists()) {
			logFile.delete();
		}

		Fork.setLoggingEnabled(true);
		Fork.setJvmOptionsForAll(new String[] {
				//--- Java logging configuration file
				"-Djava.util.logging.config.file=./samples/logging_swing.properties",
				//--- enable remote debugging
				//"-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n",
				//--- enable JMX monitoring 
				//"-Dcom.sun.management.jmxremote",
				//"-Dcom.sun.management.jmxremote.authenticate=false",
				//"-Dcom.sun.management.jmxremote.ssl=false",
				//"-Dcom.sun.management.jmxremote.port=9999",
		});

		JFrame frame1 = new JFrame("JFork - Duke A");
		frame1.setSize(400, 300);
		frame1.setLocation(300, 300);
		JFrame frame2 = new JFrame("JFork - Duke B");
		frame2.setSize(400, 300);
		frame2.setLocation(700, 300);
		JFrame fraem3 = new JFrame("JFork - Duke C");
		fraem3.setSize(400, 300);
		fraem3.setLocation(300, 600);
		JFrame frame4 = new JFrame("JFork - Duke D");
		frame4.setSize(400, 300);
		frame4.setLocation(700, 600);
		
		DukePaintTask popup1 = new DukePaintTask(frame1);
		DukePaintTask popup2 = new DukePaintTask(frame2);
		DukePaintTask popup3 = new DukePaintTask(fraem3);
		DukePaintTask popup4 = new DukePaintTask(frame4);
		try {
			JFrameFork fork1 = new JFrameFork(popup1);
			JFrameFork fork2 = new JFrameFork(popup2);
			JFrameFork fork3 = new JFrameFork(popup3);
			JFrameFork fork4 = new JFrameFork(popup4);
			fork1.setVisible(true);
			fork2.setVisible(true);
			fork3.setVisible(true);
			fork4.setVisible(true);
			
			Random rand = new Random();
			while (! isAllFinished(fork1, fork2, fork3, fork4)) {
				Point pos = new Point(rand.nextInt(300), rand.nextInt(200));
				int paintIntervalMillis = 1000;
				if (!fork1.isFinished()) {
					paintDuke(fork1, pos);
				} 
				if (!fork2.isFinished()) {
					paintDuke(fork2, pos);
				} 
				if (!fork3.isFinished()) {
					paintDuke(fork3, pos);
				} 
				if (!fork4.isFinished()) {
					paintDuke(fork4, pos);
				} 
				Thread.sleep(paintIntervalMillis);
			}
		} catch (Exception e) {
			e.printStackTrace(); // unexpected error
		}
	}

	private static void paintDuke(JFrameFork fork1, Point pos)
			throws IOException {
		try {
			Method paintDuke = DukePaintTask.class.getMethod("paintDuke", Point.class);
			fork1.call(paintDuke, pos);
		} catch (Exception e) {
			System.out.printf("Guess frame of fork '%s' was closed, try to continue.",
					fork1.toString());
		}
	}

	private static boolean isAllFinished(JFrameFork... forks) {
		for (JFrameFork f : forks) {
			if (!f.isFinished()) {
				return false;
			}
		}
		return true;
	}

}
