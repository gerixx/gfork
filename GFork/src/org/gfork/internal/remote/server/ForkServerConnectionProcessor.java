package org.gfork.internal.remote.server;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gfork.Fork;
import org.gfork.internal.remote.Command;
import org.gfork.internal.remote.Connection;
import org.gfork.internal.remote.client.ForkClient;

public class ForkServerConnectionProcessor extends Thread {

	private static final Logger LOG = Logger.getLogger(ForkServerConnectionProcessor.class.getName());

	private Connection con;
	private boolean stop;
	private String nextCommand;

	private String className;

	@SuppressWarnings("rawtypes")
	private Fork fork;

	public ForkServerConnectionProcessor(Connection con) {
		super();
		this.con = con;
	}

	@Override
	public void run() {
		while (!isStop()) {
			Command nextCommand = readNextCommand();
			switch (nextCommand) {
			case run:
				runFork();
				break;
			case runMethod:
				runMethodFork();
				break;
			case getMethodReturnValue:
				getMethodReturnValue();
				break;
			case waitFor:
				waitForFork();
				break;
			case getTask:
				getTask();
				break;
			case getStdErr:
				getStdErr();
				break;
			case getStdOut:
				getStdOut();
				break;
			case getExitValue:
				getExitValue();
				break;
			case NAC:
				break; // ignore
			case connect:
			case connectOk:
			case runOk:
			case waitForFinished:
			case waitForError:
				throw new RuntimeException("Unexpected command: " + nextCommand);
			default:
				break;
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void runFork() {
		try {
			LOG.info("Run Fork");
			Serializable task = ForkClient.readObject(con.getSocketData().getInputStream());
			className = task.getClass().getName();
			LOG.info(getLogContext() + " - run '" + className + "'");
			Constructor<Fork> constructor1 = Fork.class.getConstructor(Serializable.class);
			fork = constructor1.newInstance(task);
			fork.execute();
			this.con.getSocketControlWriter().println(Command.runOk);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void runMethodFork() {
		try {
			LOG.info("Run Method Fork");
			Serializable task = ForkClient.readObject(con.getSocketData().getInputStream());
			String methodName = (String) ForkClient.readObject(con.getSocketData().getInputStream());
			Serializable[] methodArgs = (Serializable[]) ForkClient.readObject(con.getSocketData().getInputStream());
			className = task.getClass().getName();
			Class<?>[] parameterTypes = getMethodParameterTypes(methodArgs);
			Method method = task.getClass().getMethod(methodName, parameterTypes);
			LOG.info(getLogContext() + " - run '" + className + "." + method.getName() + "'");
			Constructor<Fork> constructor = Fork.class.getConstructor(Serializable.class, Method.class, Serializable[].class);
			fork = constructor.newInstance(task, method, methodArgs);
			fork.execute();
			this.con.getSocketControlWriter().println(Command.runOk);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Class<?>[] getMethodParameterTypes(Serializable[] methodArgs) {
		List<Class<?>> typeList = new ArrayList<>();
		for (Serializable serializable : methodArgs) {
			typeList.add(serializable.getClass());
		}
		return typeList.toArray(new Class<?>[typeList.size()]);
	}

	private Command readNextCommand() {
		nextCommand = con.getSocketControlScanner().nextLine();
		try {
			return Command.valueOf(nextCommand);
		} catch (Exception e) {
			LOG.log(Level.FINE, () -> getLogContext() + " - invalid command received: '" + nextCommand + "'");
			return Command.NAC;
		}
	}

	private void waitForFork() {
		try {
			LOG.info(getLogContext() + " - wait for");
			int statusCode = fork.waitFor();
			this.con.getSocketControlWriter().println(Command.waitForFinished);
			this.con.getSocketControlWriter().println("statusCode=" + statusCode);
			LOG.info(getLogContext() + " - finsihed, statusCodd=" + statusCode);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, getLogContext(), e);
			this.con.getSocketControlWriter().println(Command.waitForError);
			this.con.getSocketControlWriter().println(getLogContext() + " - " + e.getMessage());
		}
	}

	private void getTask() {
		try {
			LOG.info(getLogContext() + " - get task");
			ForkClient.writeObject(fork.getTask(), con.getSocketData().getOutputStream());
		} catch (Exception e) {
			LOG.log(Level.SEVERE, getLogContext(), e);
		}
	}

	private void getStdErr() {
		try {
			ForkClient.writeObject(fork.getStdErr(), con.getSocketData().getOutputStream());
		} catch (Exception e) {
			LOG.log(Level.SEVERE, getLogContext(), e);
		}
	}
	
	private void getStdOut() {
		try {
			ForkClient.writeObject(fork.getStdOut(), con.getSocketData().getOutputStream());
		} catch (Exception e) {
			LOG.log(Level.SEVERE, getLogContext(), e);
		}
	}

	private void getExitValue() {
		try {
			ForkClient.writeObject(fork.getExitValue(), con.getSocketData().getOutputStream());
		} catch (Exception e) {
			LOG.log(Level.SEVERE, getLogContext(), e);
		}
	}

	private void getMethodReturnValue() {
		try {
			ForkClient.writeObject(fork.getReturnValue(), con.getSocketData().getOutputStream());
		} catch (Exception e) {
			LOG.log(Level.SEVERE, getLogContext(), e);
		}
	}

	public boolean isStop() {
		return stop;
	}

	public void setStop(boolean stop) {
		this.stop = stop;
	}

	private String getLogContext() {
		return "Run Fork: className = '" + className + "'";
	}
}
