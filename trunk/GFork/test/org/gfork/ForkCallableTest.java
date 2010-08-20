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

import org.gfork.ForkCallable.CallHandler;
import org.junit.Before;
import org.junit.Test;


public class ForkCallableTest {

	private int asyncCallCounter;

	@Before
	public void init() {
		// configure logging before every test because some of the tests disable it
		Fork.setLoggingEnabled(true);
		Fork.setJvmOptionsForAll(new String[] {
				"-Djava.util.logging.config.file=./logging_forkRunner.properties",
		});
	}

	@Test
	public void testSimpleCallable() throws Exception {
		MyCallableTask task = new MyCallableTask();
		ForkCallable<MyCallableTask> fork = new ForkCallable<MyCallableTask>(task);
		fork.execute();
		fork.call(task.getClass().getMethod("set", String.class), "hello callable");
		String value = fork.call(String.class, task.getClass().getMethod("get"));
		assertEquals("hello callable", value);
		fork.shutdown();
	}
	
	@Test
	public void testAsyncCalls() throws Exception {
		asyncCallCounter = 0;
		MyCallableTask task = new MyCallableTask();
		ForkCallable<MyCallableTask> fork = new ForkCallable<MyCallableTask>(task);
		fork.execute();
		
		// async calls to fork task
		CallHandler<org.gfork.types.Void> callbackSet = new ForkCallable.CallHandler<org.gfork.types.Void>() {

			@Override
			public void onException(Exception e) {
				fail("not expected: " + e.getMessage());
			}

			@Override
			public void onReturn(org.gfork.types.Void returnValue) {
				asyncCallCounter++;
			}
		};
		CallHandler<String> callbackGet = new ForkCallable.CallHandler<String>() {
			
			@Override
			public void onException(Exception e) {
				fail("not expected: " + e.getMessage());
			}
			
			@Override
			public void onReturn(String returnValue) {
				assertEquals("counter=0", returnValue);
				asyncCallCounter++;
			}
		};
		
		assertEquals(0, asyncCallCounter);
		
		fork.callAsync(callbackSet, org.gfork.types.Void.class, 
				task.getClass().getMethod("set", String.class), 
				"counter=" + asyncCallCounter);
		fork.callAsync(callbackGet, String.class, 
				task.getClass().getMethod("get"));
		
		Thread.sleep(2000);
		assertEquals(2, asyncCallCounter);
	}
}

