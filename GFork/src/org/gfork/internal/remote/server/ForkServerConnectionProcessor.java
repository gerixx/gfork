package org.gfork.internal.remote.server;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

	private Map<String, ForkServerConnectionProcessor> connections;

	public ForkServerConnectionProcessor(ConnectionServerSide con) {
		super();
		setName("ForkServerConnectionProcessor id=" + con.getId());
		this.con = con;
	}

	@Override
	public void run() {
		while (!isStop()) {
			try {
				Command nextCommand = readNextCommand();
				switch (nextCommand) {
				case run:
					runFork();
					break;
				case kill:
					killFork();
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
				case isFinished:
					isFinished();
					break;
				case connectClose:
					connectClose();
					break;
				case NAC:
					break; // ignore
				default:
					throw new RuntimeException("Unexpected command: " + nextCommand);
				}
			} catch (Exception e) {
				LOG.log(Level.SEVERE, e.getMessage(), e);
			} finally {
				connections.remove(con.getId());
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void runFork() {
		try {
			LOG.info("Run Fork");
			Serializable task = ForkClient.readObject(con.getSocketData().getInputStream());
			Fork.setJvmOptionsForAll((List)ForkClient.readObject(con.getSocketData().getInputStream()));
			List<String> vmOptions = (List)ForkClient.readObject(con.getSocketData().getInputStream());
			className = task.getClass().getName();
			LOG.info(getLogContext() + " - run '" + className + "'");
			Constructor<Fork> constructor1 = Fork.class.getConstructor(Serializable.class);
			fork = constructor1.newInstance(task);
			fork.setJvmOptions(vmOptions);
			fork.execute();
			this.con.getSocketControlWriter().println(Command.runOk);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	private void killFork() {
		try {
			LOG.info(getLogContext() + " - kill '" + className + "'");
			fork.kill();
			this.con.getSocketControlWriter().println(Command.killOk);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void runMethodFork() {
		try {
			LOG.info("Run Method Fork");
			InputStream inputStream = con.getSocketData().getInputStream();
			Serializable task = ForkClient.readObject(inputStream);
			Fork.setJvmOptionsForAll((List)ForkClient.readObject(inputStream));
			List<String> vmOptions = (List)ForkClient.readObject(con.getSocketData().getInputStream());
			String methodName = (String) ForkClient.readObject(inputStream);
			Serializable[] methodArgs = (Serializable[]) ForkClient.readObject(inputStream);
			className = task.getClass().getName();
			Class<?>[] parameterTypes = getMethodParameterTypes(methodArgs);
			Method method = task.getClass().getMethod(methodName, parameterTypes);
			LOG.info(getLogContext() + " - run '" + className + "." + method.getName() + "'");
			Constructor<Fork> constructor = Fork.class.getConstructor(Serializable.class, Method.class, Serializable[].class);
			fork = constructor.newInstance(task, method, methodArgs);
			fork.setJvmOptions(vmOptions);
			fork.execute();
			this.con.getSocketControlWriter().println(Command.runOk);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, e.getMessage(), e);
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

	private void connectClose() {
		setStop(true);
		if (fork.isExecuting()) {
			try {
				fork.kill();
			} catch (Exception e) {
				LOG.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		this.con.getSocketControlWriter().println(Command.connectCloseOk);
		try {
			con.close();
		} catch (Exception e) {
			LOG.log(Level.SEVERE, e.getMessage(), e);
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

	private void isFinished() {
		try {
			ForkClient.writeObject(fork.isFinished(), con.getSocketData().getOutputStream());
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

	private boolean isStop() {
		return stop;
	}

	private void setStop(boolean stop) {
		this.stop = stop;
	}

	private String getLogContext() {
		return "Run Fork: className = '" + className + "'";
	}

	public void start(Map<String, ForkServerConnectionProcessor> connections) {					
		this.connections = connections;
		connections.put(con.getId(), this);
		start();
	}
}
