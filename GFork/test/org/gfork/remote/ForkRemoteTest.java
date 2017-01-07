package org.gfork.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.gfork.Fork;
import org.gfork.ForkTest;
import org.gfork.tasks.Task01;
import org.gfork.types.Void;
import org.junit.Test;

public class ForkRemoteTest extends ForkTest {

	@Test
	public void testRemoteSimpleDefaultPort() throws Exception {
		Fork<Task01, Void> f = new Fork<Task01, Void>(new Task01());

		f.execute("localhost");

		f.waitFor();
		System.out.printf("std err: '%s'%n", f.getStdErr());
		System.out.printf("std out: '%s'&n", f.getStdOut());

		assertFalse(f.isError());
		assertEquals("Task01.run()" + Fork.NL, f.getStdOut());
		assertFalse(f.isError());
	}
}
