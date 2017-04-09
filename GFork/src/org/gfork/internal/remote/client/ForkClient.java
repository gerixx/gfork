package org.gfork.internal.remote.client;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gfork.internal.remote.Command;
import org.gfork.internal.remote.ReplyData;
import org.gfork.remote.TimeoutException;
import org.gfork.remote.server.ForkServer;

/**
 * Client has 2 sockets to Server: control, data
 * 
 * Server implements class loader Server loads class on demand from Client
 * Server caches classes
 * 
 * @author geri
 *
 */
public class ForkClient {

	private static final Logger LOG = Logger.getLogger(ForkClient.class.getName());

	private int port = ForkServer.DEFAULT_PORT;

	private InetSocketAddress serverAddress;

	private static Map<String, ForkClient> forks = new HashMap<>();

	private ConnectionClientSide con;

	private String className;

	private Integer statusCode;

	private Object taskChanged;

	private Method method;

	private Object[] methodArgs;

	private ArrayList<String> vmOptionsForAll;

	private ArrayList<String> vmOptions;

	public static ForkClient connect(String host) throws Exception {
		ForkClient forkClient = new ForkClient(host);
		forkClient.connect();
		forks.put(host, forkClient);
		return forkClient;
	}

	private ForkClient() {
		// not needed
	}

	private ForkClient(String serverAddress) {
		this.serverAddress = parseAddress(serverAddress);
	}

	private InetSocketAddress parseAddress(String serverAddress) {
		String[] addrPort = serverAddress.split(":");
		String host = addrPort[0];
		port = addrPort.length > 1 ? Integer.parseInt(addrPort[1]) : ForkServer.DEFAULT_PORT;
		return new InetSocketAddress(host, port);
	}

	private void connect() throws Exception {
		con = new ConnectionClientSide(serverAddress, UUID.randomUUID().toString());
		handShake();
	}

	public void run(Serializable task) throws Exception {
		this.className = task.getClass().getName();
		if (method == null) {
			con.getSocketControlWriter().println(Command.run);
			writeObject(task, con.getSocketData().getOutputStream());
			writeObject(vmOptionsForAll, con.getSocketData().getOutputStream());
			writeObject(vmOptions, con.getSocketData().getOutputStream());
		} else {
			con.getSocketControlWriter().println(Command.runMethod);
			writeObject(task, con.getSocketData().getOutputStream());
			writeObject(vmOptionsForAll, con.getSocketData().getOutputStream());
			writeObject(vmOptions, con.getSocketData().getOutputStream());
			writeObject(method.getName(), con.getSocketData().getOutputStream());
			writeObject(method.getParameterTypes(), con.getSocketData().getOutputStream());
			writeObject(methodArgs, con.getSocketData().getOutputStream());
		}
		checkReply(Command.runOk, "Remote run of class '" + className + "' failed.");
	}

	public void kill() {
		try {
			con.getSocketControlWriter().println(Command.kill);
			checkReply(Command.killOk, "Remote kill of class '" + className + "' failed.");
			close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public int waitFor() {
		if (statusCode == null) {
			try {
				con.getSocketControlWriter().println(Command.waitFor);
				checkReply(Command.waitForFinished, "Remote waitFor failed.");
			} catch (Exception e) {
				String serverError = ForkServer.readReplyControl(con);
				LOG.log(Level.SEVERE, "Error waitFor class '" + className + "', server error: " + serverError, e);
				throw new RuntimeException(e);
			}
			statusCode = Integer.parseInt(readPropertyFromContorl("statusCode"));
		}
		return statusCode;
	}

	public Object getTask() {
		if (statusCode == null) {
			throw new IllegalStateException("waitFor was not called");
		}
		if (taskChanged == null) {
			try {
				con.getSocketControlWriter().println(Command.getTask);
				taskChanged = readObject(con.getSocketData().getInputStream());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return taskChanged;
	}

	private String readPropertyFromContorl(String name) {
		String nameValuePair = ForkServer.readReplyControl(con);
		String[] nameValueArray = nameValuePair.split("=");
		assert nameValueArray.length == 2;
		assert nameValueArray[0].equals(name);
		return nameValueArray[1];
	}

	public void close() {
		try {
			con.getSocketControlWriter().println(Command.connectClose);
			checkReply(Command.connectCloseOk, "Close connection for class '" + className + "' failed.");
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			con.close();
		}
	}

	private void handShake() throws Exception {
		con.getSocketControlWriter().println(Command.connect.toString());
		con.getSocketControlWriter().println(con.getId());

		Socket socketData = new Socket(InetAddress.getLocalHost(), ForkServer.DEFAULT_PORT);
		socketData.getOutputStream().write(con.getId().getBytes());
		socketData.getOutputStream().flush();
		con.setSocketData(socketData);

		checkReply(con.getId(), "Invalid ID response of server.");
		checkReply(Command.connectOk, "Invalid command reply of server.");
	}

	private void checkReply(String token, String msg) throws Exception {
		ReplyData runReply = ForkServer.readReplyControl(token, con);
		if (runReply.isTimedOut()) {
			throw new TimeoutException("Timeout: " + msg + " Expected reply '" + token + "'");
		}
		if (!runReply.isExpectedToken()) {
			throw new IllegalStateException(
					msg + " Expected reply '" + token + "', got '" + runReply.getReplyToken() + "'");
		}
	}

	// TODO msg as lambda for better performance
	private void checkReply(Command cmd, String msg) throws Exception {
		checkReply(cmd.toString(), msg);
	}

	public static void writeObject(Serializable obj, OutputStream outputStream) throws Exception {
		ObjectOutputStream objOut = new ObjectOutputStream(outputStream);
		objOut.writeObject(obj);
		objOut.flush();
	}

	public static Object readObject(InputStream inputStream) throws Exception {
		ObjectInputStream objIn = new ObjectInputStream(inputStream);
		return objIn.readObject();
	}

	public String getStdErr() {
		try {
			con.getSocketControlWriter().println(Command.getStdErr);
			return (String)readObject(con.getSocketData().getInputStream());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String getStdOut() {
		try {
			con.getSocketControlWriter().println(Command.getStdOut);
			return (String)readObject(con.getSocketData().getInputStream());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public int getExitValue() {
		try {
			con.getSocketControlWriter().println(Command.getExitValue);
			return (Integer)readObject(con.getSocketData().getInputStream());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Serializable getReturnValue() {
		try {
			con.getSocketControlWriter().println(Command.getMethodReturnValue);
			return (Serializable) readObject(con.getSocketData().getInputStream());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isFinished() {
		try {
			con.getSocketControlWriter().println(Command.isFinished);
			return (boolean)readObject(con.getSocketData().getInputStream());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public void setMethodArgs(Object[] methodArgs) {
		this.methodArgs = methodArgs;
	}

	public void setVmOptionsForAll(ArrayList<String> vmOptionsForAll) {
		this.vmOptionsForAll = vmOptionsForAll;
	}

	public void setVmOptions(ArrayList<String> vmOptions) {
		this.vmOptions = vmOptions;
	}

	public boolean isClosed() {
		return con.isClosed();
	}
}
