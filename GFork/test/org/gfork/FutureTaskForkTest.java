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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.gfork.Fork;
import org.gfork.FutureTaskFork;
import org.gfork.tasks.Task01;
import org.gfork.tasks.TaskCallable;
import org.gfork.types.MethodArgumentsException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class FutureTaskForkTest {
	
	@BeforeClass
	public static void initForAll() {
		File logFile = new File("./tmp/forkRunner1.log");
		if (logFile.exists()) {
			logFile.delete();
		}
	}
	
	@Before
	public void init() {
		// configure logging before every test because some of the tests disable it
		Fork.setLoggingEnabled(true);
		Fork.setJvmOptionsForAll(new String[] {
				"-Djava.util.logging.config.file=./logging_forkRunner.properties",
		});
	}

	@Test
	public void testSubmitRunnable() throws SecurityException, IOException, NoSuchMethodException, MethodArgumentsException, InterruptedException, ExecutionException {
		FutureTaskFork<Integer> task = new FutureTaskFork<Integer>(new Task01(), Integer.valueOf(0));
		ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 5000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(2));
		
		executor.execute(task);
		
		assertEquals(Integer.valueOf(0), task.get());
		assertTrue(task.isDone());
		assertFalse(task.isCancelled());
	}
	
	@Test
	public void testSubmitCallable() throws SecurityException, IOException, NoSuchMethodException, MethodArgumentsException, InterruptedException, ExecutionException {
		FutureTaskFork<String> task = new FutureTaskFork<String>(new TaskCallable());
		ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 5000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(2));
		
		executor.execute(task);
		
		assertEquals("callable called", task.get());
		assertTrue(task.isDone());
		assertFalse(task.isCancelled());
	}
}
