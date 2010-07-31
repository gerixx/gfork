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

import org.junit.Before;
import org.junit.Test;


public class ForkCallableTest {

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
}
