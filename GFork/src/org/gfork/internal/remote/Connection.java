package org.gfork.internal.remote;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public abstract class Connection {

	private Socket socketControl;
	private Socket socketData;
	private PrintWriter socketControlWriter;
	private Scanner socketControlScanner;
	private String id;
	private ConnectionWriter connectionControlWriter;
	private boolean isClosed;

	public Connection(Socket socket) throws Exception {
		handleConnect(socket);
		id = socketControlScanner.next();
	}

	public Connection(Socket socket, String id) throws Exception {
		this.id = id;
		handleConnect(socket);
	}

	public Connection(InetSocketAddress serverAddress, String id) throws Exception, Exception {
		this(new Socket(serverAddress.getAddress(), serverAddress.getPort()), id);
	}

	private void handleConnect(Socket socket) throws IOException {
		socketControl = socket;
		socketControlWriter = new PrintWriter(socket.getOutputStream(), true);
		connectionControlWriter = new ConnectionWriter(socketControlWriter);
		socketControlScanner = new Scanner(socket.getInputStream(), "UTF-8");
		socketControlScanner.useDelimiter("[\\r\\n]+");
	}

	private void checkStatus() {
		if (isClosed()) {
			throw new IllegalStateException(getConnectionLogPrefix() + "connection is closed, id=" + id);
		}
	}

	public void replyMsgAndClose(String msg) {
		checkStatus();
		if (socketControlWriter != null) {
			socketControlWriter.println(msg);
		}
		close();
	}

	public void close() {
		checkStatus();
		this.isClosed = true;
		System.out.println(getConnectionLogPrefix() + "close connection, id=" + id);
		closeSocket(socketControl);
		socketControl = null;
		closeSocket(socketData);
		socketData = null;
		socketControlWriter = null;
	}

	public boolean isClosed() {
		return this.isClosed;
	}

	public String getId() {
		return this.id;
	}

	public ConnectionWriter getSocketControlWriter() {
		checkStatus();
		return this.connectionControlWriter;
	}

	public Socket getSocketControl() {
		checkStatus();
		return this.socketControl;
	}

	public Scanner getSocketControlScanner() {
		checkStatus();
		return this.socketControlScanner;
	}

	public Socket getSocketData() {
		checkStatus();
		return this.socketData;
	}

	public void setSocketData(Socket socketData) {
		checkStatus();
		this.socketData = socketData;
	}

	public class ConnectionWriter {

		private PrintWriter out;

		public ConnectionWriter(PrintWriter out) {
			this.out = out;
		}

		public void println(String line) {
			checkStatus();
			System.out.println(getConnectionLogPrefix() + line);
			out.println(line);
		}

		public void println(Command cmd) {
			checkStatus();
			this.println(cmd.toString());
		}
	}

	protected abstract String getConnectionLogPrefix();

	public static void closeSocket(Socket s) {
		try {
			if (s != null) {
				s.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
