package org.gfork.internal.remote.server;

import java.io.Serializable;
import java.lang.reflect.Constructor;
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
				LOG.info("Run Fork");
				runFork();
				break;
			case waitFor:
				LOG.info(getLogContext() + " - wait for");
				waitForFork();
				break;
			case getTask:
				LOG.info(getLogContext() + " - get task");
				getTaskChanged();
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

	private Command readNextCommand() {
		nextCommand = con.getSocketControlScanner().nextLine();
		try {
			return Command.valueOf(nextCommand);
		} catch (Exception e) {
			LOG.log(Level.FINE, () -> getLogContext() + " - invalid command received: '" + nextCommand + "'");
			return Command.NAC;
		}
	}

	@SuppressWarnings("rawtypes")
	private void runFork() {
		try {
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

	private void waitForFork() {
		try {
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

	private void getTaskChanged() {
		try {
			ForkClient.writeObject(fork.getTask(), con.getSocketData().getOutputStream());
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