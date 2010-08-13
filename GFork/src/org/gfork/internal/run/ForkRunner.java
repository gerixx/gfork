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

package org.gfork.internal.run;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gfork.Linkable;
import org.gfork.types.Void;


public class ForkRunner {
	
	/**
	 * Code used to exit the fork process when task ends with an exception.
	 * Can be modified to adjust the preferred exit code to be used by {@link ForkRunner}.
	 */
	public static final int EXIT_CODE_ON_EXCEPTION = 4229;
	
	private final static Logger log = Logger.getLogger(ForkRunner.class.getName());
	
	private static Socket taskInput;
	private static Socket taskSuccessorSocket;

	public static void main(final String[] args) {
		File exceptionFile = null;
		try {
			if (! log.isLoggable(Level.SEVERE)) {
				log.setLevel(Level.SEVERE); // force logging while preparing arguments
			}
			final Arguments a = new Arguments(args);
			if (! a.loggingEnabled) {
				log.setLevel(Level.OFF);
			}
			logArgs(Level.INFO, args);
			exceptionFile = checkAndOpenFile(a.exceptionFile);
			final File taskFile = checkAndOpenFile(a.taskFile);
			final Object task = readTaskObject(taskFile);
			connectLink(a, task);
			executeTask(a, task);
			disconnectLink();
			writeTaskObject(task, taskFile);
		} catch (final Throwable e) {
			log.log(Level.SEVERE, "boot error", e);
			writeExceptionToFile(exceptionFile, e);
			System.exit(EXIT_CODE_ON_EXCEPTION);
		}
		log.info("exit");
		System.exit(0); // try to force exit
	}

	private static void connectLink(final Arguments a, final Object task) throws UnknownHostException, IOException {
		if (task instanceof Linkable) {
			final Linkable linkableTask = (Linkable) task;
			if (a.outputPort > 0) {
				taskSuccessorSocket = new Socket("127.0.0.1", a.outputPort);
				linkableTask.setSuccTaskWriteStream(taskSuccessorSocket.getOutputStream());
				linkableTask.setSuccTaskReadStream(taskSuccessorSocket.getInputStream());
				log.info(String.format("linkable task output stream set, port is %d", a.outputPort));
			}
			final ServerSocket taskPredecessorSocket = new ServerSocket(0);
			System.out.printf("port:%d%n", taskPredecessorSocket.getLocalPort());
			System.out.flush();
			log.info(String.format("linkable task listener established, port is %d", taskPredecessorSocket.getLocalPort()));
			taskInput = taskPredecessorSocket.accept();
			linkableTask.setPredTaskReadStream(taskInput.getInputStream());
			linkableTask.setPredTaskWriteStream(taskInput.getOutputStream());
			taskPredecessorSocket.close();
			log.info("linkable task input stream set");
			linkableTask.init();
			log.info("linkable task initialized");
		} else {
			System.out.println("port:null");
		}
	}

	private static void executeTask(final Arguments a, final Object task)
			throws NoSuchMethodException, IllegalAccessException,
			InvocationTargetException, FileNotFoundException, IOException,
			ClassNotFoundException {
		final Method method;
		final Object retVal;
		log.info(String.format("task starting: method %s(void)", a.methodName));
		if (a.isMethodWithNoParameters) {
			method = task.getClass().getMethod(a.methodName);
			retVal = method.invoke(task);
		} else {
			final MethodArgTypesAndValues typesAndValues = readMethodTypesAndValues(a);
			logInvokeMethodTypesAndValues(Level.INFO, a.methodName, typesAndValues);
			method = task.getClass().getMethod(a.methodName, typesAndValues.types);
			retVal = method.invoke(task, typesAndValues.values);
		}
		log.info(String.format("task finished: method %s(void)", a.methodName));
		writeReturnValue(method, a.methodReturnValueFile, retVal);
	}

	private static void disconnectLink() throws IOException {
		if (taskSuccessorSocket != null) {
			taskSuccessorSocket.close();
		}
		if (taskInput != null) {
			taskInput.close();
		}
	}

	private static Object readTaskObject(final File taskFile)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		final FileInputStream fin = new FileInputStream(taskFile);
		final ObjectInputStream oin = new ObjectInputStream(fin);
		final Object task = oin.readObject();
		fin.close();
		return task;
	}

	private static MethodArgTypesAndValues readMethodTypesAndValues(final Arguments a)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		final File taskArgsFile = checkAndOpenFile(a.methodArgsFile);
		final FileInputStream afin = new FileInputStream(taskArgsFile);
		final ObjectInputStream aoin = new ObjectInputStream(afin);
		final Class<?>[] types = (Class<?>[]) aoin.readObject();
		final Object[] values = (Object[]) aoin.readObject();
		afin.close();
		return new MethodArgTypesAndValues(types, values);
	}

	private static void writeReturnValue(final Method method, final String retValFileName, final Object retVal)
			throws FileNotFoundException, IOException {
		final File retValFile = checkAndOpenFile(retValFileName);
		if (! method.getReturnType().equals(Void.class) && retVal != null) {
			final FileOutputStream fout = new FileOutputStream(retValFile);
			final ObjectOutputStream oout = new ObjectOutputStream(fout);
			oout.writeObject(retVal);
			fout.close();
		} else {
			retValFile.delete(); // only for void
		}
	}

	private static void writeTaskObject(final Object task, final File taskFile) throws IOException {
		final FileOutputStream fout = new FileOutputStream(taskFile);
		final ObjectOutputStream oout = new ObjectOutputStream(fout);
		oout.writeObject(task);
		oout.close();
	}

	private static void writeExceptionToFile(final File exceptionFile, final Throwable e) {
		if (exceptionFile == null) {
			return;
		}
		final Throwable ex;
		if (e instanceof InvocationTargetException && e.getCause() != null) {
			ex = e.getCause();
		} else {
			ex = e;
		}
		ex.printStackTrace(System.err);
		try {
			writeExceptionObject(ex, exceptionFile);
		} catch (final Exception e1) {
			e1.printStackTrace(System.err);
		}
	}

	private static void writeExceptionObject(final Throwable e, final File exceptionFile) throws IOException {
		final FileOutputStream fout = new FileOutputStream(exceptionFile);
		final ObjectOutputStream oout = new ObjectOutputStream(fout);
		oout.writeObject(e);
		oout.close();
	}

	private static File checkAndOpenFile(final String path) {
		final File taskFile = new File(path);
		if (! taskFile.exists()) {
			throw new RuntimeException(String.format("File does not exist (%s)", path));
		}
		if (! taskFile.canRead()) {
			throw new RuntimeException(String.format("Cannot read file (%s)", path));
		}
		if (! taskFile.canWrite()) {
			throw new RuntimeException(String.format("Cannot write file (%s)", path));
		}
		return taskFile;
	}

	private static void logArgs(final Level level, final String[] args) {
		if (log.isLoggable(level)) {
			final StringBuilder msg = new StringBuilder();
			msg.append("ForkRunner arguments: count ").append(args.length);
			for (int i = 0; i < args.length; i++) {
				msg.append("\n  arg ").append(i).append(": ").append(args[i]);
			}
			log.log(level, msg.toString());
		}
	}

	private static void logInvokeMethodTypesAndValues(final Level level, final String methodName,
			final MethodArgTypesAndValues typesAndValues) {
		if (log.isLoggable(level)) {
			final StringBuilder msg = new StringBuilder();
			msg.append("invoke method ").append(methodName).append('(');
			for (int i = 0; i < typesAndValues.types.length; i++) {
				msg.append("\n  ");
				msg.append(typesAndValues.types[i].getName()).append(' ');
				msg.append('\'').append(typesAndValues.values[i].toString()).append('\'');
				msg.append('(').append(typesAndValues.values[i].getClass()).append(')');
			}
			msg.append(')');
			log.log(level, msg.toString());
		} 
	}

	private static class MethodArgTypesAndValues {
		final Class<?>[] types;
		final Object[] values;
		
		public MethodArgTypesAndValues(final Class<?>[] types, final Object[] values) {
			this.types = types;
			this.values = values;
		}
	}
	
	private static class Arguments {
		public boolean isMethodWithNoParameters;
		
		public String taskFile;
		public String exceptionFile;
		public boolean loggingEnabled;
		public String methodReturnValueFile;
		public String methodName;
		public String methodArgsFile;
		public int outputPort;

		public Arguments(final String[] args) {
			taskFile = args[0];
			exceptionFile = args[1];
			loggingEnabled = Boolean.parseBoolean(args[2]);
			methodReturnValueFile = args[3];
			methodName = args[4];
			methodArgsFile = args[5];
			if (methodArgsFile.equals(".nomethodargs")) {
				isMethodWithNoParameters = true;
			}
			if (args.length > 6) {
				outputPort = Integer.parseInt(args[6]);
			}
		}
	}
}
