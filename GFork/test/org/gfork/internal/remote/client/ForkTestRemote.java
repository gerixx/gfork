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

package org.gfork.internal.remote.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.gfork.Fork;
import org.gfork.helpers.ForkListenerAdapter;
import org.gfork.internal.run.ForkRunner;
import org.gfork.remote.server.ForkServer;
import org.gfork.remote.server.ForkServerTest;
import org.gfork.tasks.KeyValue;
import org.gfork.tasks.Task01;
import org.gfork.tasks.Task02;
import org.gfork.tasks.When;
import org.gfork.types.MethodArgumentsException;
import org.gfork.types.Void;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class ForkTestRemote {

	private String result = "?";

	private Throwable exception1;
	private int errorCode = -1;

	private boolean onFinished;

	private boolean notifyKill;

	@BeforeClass
	public static void initForAll() throws IOException {
		File stdout = new File("./tmp/stdoutTest.txt");
		if (!stdout.exists()) {
			File tmpDir = new File("./tmp");
			if (!tmpDir.exists()) {
				tmpDir.mkdir();
			}
			stdout.createNewFile();
		}
	}

	@Before
	public void init() {
		ForkServerTest.startLocalDefaultForkServer();
		// configure logging before every test because some of the tests disable
		// it
		Fork.setLoggingEnabled(true);
		Fork.setJvmOptionsForAll(new String[] { "-Djava.util.logging.config.file=./logging_forkRunner.properties", });
	}

	@After
	public void endTest() throws Exception {
		ForkServer.stop();
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testCreateForkByReflection() throws Exception {
		Constructor<Fork> constructor1 = Fork.class.getConstructor(Serializable.class);
		Task01 task = new Task01();
		assertNull(task.getState());

		Fork f = constructor1.newInstance(task);
		f.execute();
		f.waitFor();
		System.out.printf("std err: '%s'%n", f.getStdErr());
		System.out.printf("std out: '%s'&n", f.getStdOut());

		assertFalse(f.isError());
		assertEquals("Task01.run()" + Fork.NL, f.getStdOut());
		assertFalse(f.isError());
		Task01 taskAfterRun = (Task01) f.getTask();
		assertEquals("executed", taskAfterRun.getState());
	}

	@Test
	public void testDefaultTaskMethod() throws Exception {
		Fork<Task01, Void> f = new Fork<Task01, Void>(new Task01());

		f.execute("localhost");

		int status = f.waitFor();
		assertEquals(0, status);
		// System.out.printf("std err: '%s'%n", f.getStdErr());
		// System.out.printf("std out: '%s'&n", f.getStdOut());
		//
		// assertFalse(f.isError());
		// assertEquals("Task01.run()" + Fork.NL, f.getStdOut());
		// assertFalse(f.isError());
		assertEquals("executed", f.getTask().getState());
	}

	@Ignore
	@Test
	public void testSimpleMethod() throws Exception {
		System.out.println("ForkTest.testSimpleMethod()");
		Date date = new Date();
		Fork<Task02, Date> f = new Fork<Task02, Date>(new Task02(date), Task02.class.getMethod("getDate"));

		f.execute();

		if (f.isError()) {
			System.out.print(f.getStdErr());
		}
		assertEquals(date, f.getReturnValue());
		assertFalse(f.isError());
	}

	@Ignore
	@Test
	public void testMethodWithArgsAndReturnValue() throws Exception {
		System.out.println("ForkTest.testMethodWithArgsAndReturnValue()");
		Fork<Task02, Date> f = new Fork<Task02, Date>(new Task02(null), Task02.class.getMethod("getDate", When.class),
				When.TOMORROW);

		f.execute();

		if (f.isError()) {
			System.out.print(f.getStdErr());
		}
		Calendar cal = GregorianCalendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, 1);
		assertEquals(cal.get(Calendar.DAY_OF_MONTH), getDay(f.getReturnValue()));
		assertFalse(f.isError());
	}

	@Ignore
	@Test
	public void testMethodWithPrimitiveArgOverload01() throws Exception {
		System.out.println("ForkTest.testMethodWithPrimitiveArgOverload01()");
		Fork<Task02, Date> f = new Fork<Task02, Date>(new Task02(null),
				Task02.class.getMethod("getValue", Integer.class), 100);

		f.execute();

		assertFalse(f.isError());
		assertEquals("getValue(Integer) - ok", f.getReturnValue());
	}

	@Ignore
	@Test
	public void testMethodWithPrimitiveArgOverload02() throws Exception {
		System.out.println("ForkTest.testMethodWithPrimitiveArgOverload02()");
		Task02 task = new Task02(null);
		Fork<Task02, Date> f = new Fork<Task02, Date>(task, task.getClass().getMethod("getValue", int.class), 100);

		f.execute();

		assertFalse(f.isError());
		assertEquals("getValue(int) - ok", f.getReturnValue());
	}

	@Ignore
	@Test
	public void testMethodPrimitiveArgs() throws Exception {
		System.out.println("ForkTest.testMethodPrimitiveArgs()");
		Fork<Task01, Boolean> f;
		Task01 task = new Task01();
		f = new Fork<Task01, Boolean>(task, task.getClass().getMethod("set", int.class), 100);
		f.execute();
		assertTestPrimitiveArgs(f);

		f = new Fork<Task01, Boolean>(task, task.getClass().getMethod("set", short.class), (short) 100);
		f.execute();
		assertTestPrimitiveArgs(f);

		f = new Fork<Task01, Boolean>(task, task.getClass().getMethod("set", long.class), 100);
		f.execute();
		assertTestPrimitiveArgs(f);

		f = new Fork<Task01, Boolean>(task, task.getClass().getMethod("set", boolean.class), true);
		f.execute();
		assertTestPrimitiveArgs(f);

		f = new Fork<Task01, Boolean>(task, task.getClass().getMethod("set", char.class), 'a');
		f.execute();
		assertTestPrimitiveArgs(f);

		f = new Fork<Task01, Boolean>(task, task.getClass().getMethod("set", byte.class), (byte) 255);
		f.execute();
		assertTestPrimitiveArgs(f);

		f = new Fork<Task01, Boolean>(task, task.getClass().getMethod("set", double.class), 3.14);
		f.execute();
		assertTestPrimitiveArgs(f);

		f = new Fork<Task01, Boolean>(task, task.getClass().getMethod("set", float.class), (float) 3.14);
		f.execute();
		assertTestPrimitiveArgs(f);
	}

	@Ignore
	@Test
	public void testMethodWithNullReturnValue() throws Exception {
		System.out.println("ForkTest.testMethodWithNullReturnValue()");
		Fork<Task01, String> f = new Fork<Task01, String>(new Task01(), Task01.class.getMethod("getNullValue"));

		f.execute();

		assertNull(f.getReturnValue());
	}

	@Ignore
	@Test
	public void testKillFork() throws Exception {
		System.out.println("ForkTest.testTerminate()");
		Fork<Task02, Void> f1 = new Fork<Task02, Void>(new Task02(null, "f1"), Task02.class.getMethod("endless"));
		Fork<Task02, Void> f2 = new Fork<Task02, Void>(new Task02(null, "f2"), Task02.class.getMethod("endless"));
		Fork<Task02, Void> f3 = new Fork<Task02, Void>(new Task02(null, "f3"), Task02.class.getMethod("endless"));

		Fork.setJvmOptionsForAll(new String[] {});
		Fork.setLoggingEnabled(false);

		f1.setStdOutWriter(new PrintWriter(System.out));
		f2.setStdOutWriter(new PrintWriter(System.out));
		f3.setStdOutWriter(new PrintWriter(System.out));

		f1.execute();
		f2.execute();
		f3.execute();

		Thread.sleep(1500);

		f1.kill();
		f2.kill();
		f3.kill();

		assertTrue(f1.isError());
		assertTrue(f2.isError());
		assertTrue(f3.isError());
		assertTrue(f1.isFinished());
		assertTrue(f2.isFinished());
		assertTrue(f3.isFinished());
	}

	@Ignore
	@Test
	public void testKillForkNotify() throws Exception {
		System.out.println("ForkTest.testKillForkNotify()");

		Fork.setJvmOptionsForAll(new String[] {});
		Fork.setLoggingEnabled(false);

		Fork<Task02, Void> f1 = new Fork<Task02, Void>(new Task02(null, "f1"), Task02.class.getMethod("endless"));

		f1.addListener(new ForkListenerAdapter<Task02, Void>() {

			@Override
			public void onFinish(Fork<Task02, Void> fork, boolean wasKilled)
					throws IllegalAccessException, InterruptedException {
				notifyKill = wasKilled;
			}

		});

		f1.setStdOutWriter(new PrintWriter(System.out));

		f1.execute();

		Thread.sleep(1500);

		assertFalse(notifyKill);
		assertFalse(f1.isFinished());
		f1.kill();

		assertTrue(f1.isFinished());
		assertTrue(notifyKill);
		System.out.println(f1.isError());
		System.out.println(f1.isException());
	}

	@Ignore
	@Test
	public void testTaskException() throws Exception {
		Fork<Task02, Void> f = new Fork<Task02, Void>(new Task02(), Task02.class.getMethod("exception"));
		f.execute();
		f.waitFor();
		assertTrue(f.isException());
		assertEquals("test exception", f.getException().getMessage());
	}

	@Ignore
	@Test
	public void testJvmOptions() throws Exception {
		System.out.println("ForkTest.testJvmOptions()");
		String libdrive = "c:\\non_existing_drive";
		Fork.setJvmOptionsForAll(new String[] { "-Xmx50m", "-Dlibrary.path=" + libdrive, });
		Fork<Task01, Boolean> f = new Fork<Task01, Boolean>(new Task01(),
				Task01.class.getMethod("checkLibPath", String.class), libdrive);
		f.execute();
		assertTrue(f.getReturnValue());
		assertTrue(f.getReturnValue()); // try multiple calls
	}

	@Ignore
	@Test
	public void testSystemProperties() throws Exception {
		System.out.println("ForkTest.testSystemProperties()");
		System.setProperty("customSysProperty1", "4testonly");
		System.setProperty("customSysProperty2", "xxxxxxx"); // overriden by JVM
																// option, see
																// below
		System.setProperty("customSysProperty3", "xxxxxxx"); // overriden by JVM
																// option for
																// all forks,
																// see below

		Fork.addJvmOptionForAll("-DcustomSysProperty3=4testonly");

		Fork<Task01, Boolean> f = new Fork<Task01, Boolean>(new Task01(),
				Task01.class.getMethod("checkSystemProperties", KeyValue[].class),
				new Serializable[] { // wrap varargs in a serializable array
						new KeyValue[] { new KeyValue("customSysProperty1", "4testonly"),
								new KeyValue("customSysProperty2", "4testonly"),
								new KeyValue("customSysProperty3", "4testonly"), } });

		f.setJvmOptions("-DcustomSysProperty2=4testonly");

		f.execute();

		assertFalse(f.isError());
		assertTrue(f.getReturnValue());
	}

	@Ignore
	@Test
	public void testStdOutFile() throws Exception {
		System.out.println("ForkTest.testStdOutFile()");
		Fork<Task02, Void> f = new Fork<Task02, Void>(new Task02(), Task02.class.getMethod("printToStdOut"));

		File stdout = new File("./tmp/stdoutTest.txt");
		stdout.delete();
		f.setStdOutWriter(new PrintWriter(stdout));

		f.execute();

		assertFalse(f.isError());
		assertFalse(f.isError()); // try multiple calls
		assertTrue(stdout.canRead());
		assertTrue("stdout size: " + stdout.length(), stdout.length() > 500);
	}

	@Ignore
	@Test
	public void testWorkingDir() throws Exception {
		System.out.println("ForkTest.testWorkingDir()");
		Fork<Task01, String> f = new Fork<Task01, String>(new Task01(), Task01.class.getMethod("getWorkingDir"));

		f.setWorkingDir(getTempFolder());
		f.execute();

		assertEquals(new File(getTempFolder()), new File(f.getReturnValue()));
	}

	@Ignore
	@Test
	public void testNotifyWithAnonymous() throws Exception {
		System.out.println("ForkTest.testNotifyWithAnonymous()");
		Fork<Task02, String> f = new Fork<Task02, String>(new Task02(), Task02.class.getMethod("delay", int.class),
				100);

		Fork.Listener<Task02, String> listener = new Fork.Listener<Task02, String>() {

			@Override
			public void onFinish(Fork<Task02, String> fork, boolean wasKilled) {
				try {
					assertEquals("delay - ok", fork.getReturnValue().substring(0, 10));
					result = "ok";
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}

			@Override
			public void onError(Fork<Task02, String> fork) throws IllegalAccessException, InterruptedException {
				fail(fork.getStdErr());
			}

			@Override
			public void onException(Fork<Task02, String> fork)
					throws IllegalAccessException, InterruptedException, IOException, ClassNotFoundException {
				fail(fork.getException().toString());
			}
		};
		f.addListener(listener);

		f.execute();

		f.waitFor();
		assertEquals("ok", result);
	}

	@Ignore
	@Test
	public void testNotifyErrorWithAdapter() throws Exception {
		System.out.println("ForkTest.testNotifyErrorWithAdapter()");
		Fork<Task02, Void> f = new Fork<Task02, Void>(new Task02(), Task02.class.getMethod("exception"));

		ForkListenerAdapter<Task02, Void> listener = new ForkListenerAdapter<Task02, Void>() {

			@Override
			public void onError(Fork<Task02, Void> fork) throws IllegalAccessException, InterruptedException {
				assertTrue(fork.isError());
			}
		};

		f.addListener(listener);

		f.execute();

		f.waitFor();
		assertTrue(f.isError());
		System.out.println(f.getStdErr());
		assertEquals("java.lang.RuntimeException: test exception", f.getStdErr().substring(0, 42));
	}

	@Ignore
	@Test
	public void testNotifyExceptionWithAdapter() throws Exception {
		System.out.println("ForkTest.testNotifyExceptionWithAdapter()");
		Fork<Task02, Void> f = new Fork<Task02, Void>(new Task02(), Task02.class.getMethod("exception"));

		ForkListenerAdapter<Task02, Void> listener = new ForkListenerAdapter<Task02, Void>() {

			@Override
			public void onError(Fork<Task02, Void> fork) throws IllegalAccessException, InterruptedException {
				assertTrue(fork.isError());
				errorCode = fork.getExitValue();
			}

			@Override
			public void onException(Fork<Task02, Void> fork) throws IllegalAccessException, InterruptedException {
				try {
					assertTrue(fork.isException());
					exception1 = fork.getException();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		};
		f.addListener(listener);

		f.execute();

		f.waitFor();
		assertEquals("test exception", exception1.getMessage());
		assertEquals(ForkRunner.EXIT_CODE_ON_EXCEPTION, errorCode);
	}

	@Ignore
	@Test
	public void testTaskFinishedStatus() throws Exception {
		Fork<Task02, String> f = new Fork<Task02, String>(new Task02(), Task02.class.getMethod("delay", int.class),
				1500);

		onFinished = false;
		ForkListenerAdapter<Task02, String> listener = new ForkListenerAdapter<Task02, String>() {

			@Override
			public void onFinish(Fork<Task02, String> fork, boolean wasKilled)
					throws IllegalAccessException, InterruptedException {
				onFinished = true;
			}

		};
		f.addListener(listener);
		assertFalse(f.isFinished());

		f.execute();

		Thread.sleep(1000);
		assertFalse(f.isFinished());
		assertFalse(onFinished);

		Thread.sleep(1500);
		assertTrue(f.isFinished());
		assertTrue(onFinished);
	}

	@Ignore
	@Test
	public void testTaskFinishedPolling() throws Exception {
		int delay = 2000;
		Fork<Task02, String> f = new Fork<Task02, String>(new Task02(), Task02.class.getMethod("delay", int.class),
				delay);

		f.execute();

		// polling
		int pollingTime = delay / 10;
		boolean finished = false;
		int cnt = 1;
		while (!finished && cnt < 20) {
			Thread.sleep(pollingTime);
			finished = f.isFinished();
			cnt++;
		}

		assertTrue(cnt >= delay / pollingTime - 1);
		assertTrue(finished);
	}

	@Ignore
	@Test
	public void printTaskEnv() throws Exception {
		System.out.println("ForkTest.printTaskEnv()");
		Fork<Task01, Void> f = new Fork<Task01, Void>(new Task01(), Task01.class.getMethod("printEnv"));

		f.setStdOutWriter(new PrintWriter(System.out));
		f.execute();
		f.waitFor();
	}

	@Ignore
	@Test(expected = MethodArgumentsException.class)
	public void negativeMethodButInvalidArgs() throws Exception {
		System.out.println("ForkTest.negativeMethodButInvalidArgs()");
		Fork<Task02, Date> f = new Fork<Task02, Date>(new Task02(null), Task02.class.getMethod("getDate"),
				When.TOMORROW);
		f.execute();
	}

	@Ignore
	@Test(expected = IllegalAccessException.class)
	public void negativeMethodWithReturnTypeVoid() throws Exception {
		System.out.println("ForkTest.negativeMethodWithReturnTypeVoid()");
		Fork<Task01, Void> f = new Fork<Task01, Void>(new Task01(), Task01.class.getMethod("voidMethod"));

		f.execute();

		f.getReturnValue();
	}

	@Ignore
	@Test(expected = NoSuchMethodException.class)
	public void negativeUnknownMethod() throws Exception {
		System.out.println("ForkTest.negativeUnknownMethod()");
		Fork<Task02, Void> f = new Fork<Task02, Void>(new Task02(null), Task02.class.getMethod("methodNotExists"));

		f.execute();
	}

	@Ignore
	@Test(expected = NoSuchMethodException.class)
	public void negativeInvalidMethodArgs() throws Exception {
		System.out.println("ForkTest.negativeInvalidMethodArgs()");
		Fork<Task02, Date> f = new Fork<Task02, Date>(new Task02(null), Task02.class.getMethod("getDate", String.class),
				"invalid param");

		f.execute();
	}

	@Ignore
	@Test(expected = ClassCastException.class)
	public void negativeReturnTypeMismatch() throws Exception {
		System.out.println("ForkTest.negativeReturnTypeMismatch()");
		Fork<Task02, String> f = new Fork<Task02, String>(new Task02(null),
				Task02.class.getMethod("getDate", When.class), When.TOMORROW);

		f.execute();

		String value = f.getReturnValue();
		System.out.println(value);
	}

	@Ignore
	@Test(expected = IllegalStateException.class)
	public void negativeStdOutFile() throws Exception {
		System.out.println("ForkTest.negativeStdOutFile()");
		Fork<Task02, Void> f = new Fork<Task02, Void>(new Task02(), Task02.class.getMethod("printToStdOut"));

		f.execute();

		File stdout = new File("./tmp/stdoutTest.txt");
		f.setStdOutWriter(new PrintWriter(stdout));
	}

	private String getTempFolder() {
		return System.getProperty("java.io.tmpdir");
	}

	private int getDay(Date date) {
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTime(date);
		int day = cal.get(Calendar.DAY_OF_MONTH);
		return day;
	}

	private void assertTestPrimitiveArgs(Fork<Task01, Boolean> f) throws InterruptedException, IllegalAccessException {
		if (f.isError()) {
			System.out.println(f.getStdErr());
		}
		assertTrue(f.getReturnValue());
	}

}
