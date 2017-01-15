package org.gfork.internal.remote;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class Connection {

	private Socket socketControl;
	private Socket socketData;
	private PrintWriter socketControlWriter;
	private Scanner socketControlScanner;
	private String id;
	private ConnectionWriter connectionControlWriter;

	public Connection(Socket socket) throws Exception {
		handleConnect(socket);
		id = socketControlScanner.next();
	}

	public Connection(Socket socket, String id) throws Exception {
		handleConnect(socket);
		this.id = id;
	}

	public Connection(InetSocketAddress serverAddress, String id) throws Exception, Exception {
		this(new Socket(serverAddress.getAddress(), serverAddress.getPort()), id);
	}

	protected void handleConnect(Socket socket) throws IOException {
		socketControl = socket;
		socketControlWriter = new PrintWriter(socket.getOutputStream(), true);
		connectionControlWriter = new ConnectionWriter(socketControlWriter);
		socketControlScanner = new Scanner(socket.getInputStream(), "UTF-8");
		socketControlScanner.useDelimiter("[\\r\\n]+");
	}

	public void replyMsgAndClose(String msg) {
		if (socketControlWriter != null) {
			socketControlWriter.println(msg);
		}
		close();
	}

	public void close() {
		closeSocket(socketControl);
		socketControl = null;
		closeSocket(socketData);
		socketData = null;
		socketControlWriter = null;
	}

	public static void closeSocket(Socket s) {
		try {
			if (s != null) {
				s.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getId() {
		return this.id;
	}

	public ConnectionWriter getSocketControlWriter() {
		return this.connectionControlWriter;
	}

	public Socket getSocketControl() {
		return this.socketControl;
	}

	public Scanner getSocketControlScanner() {
		return this.socketControlScanner;
	}

	public Socket getSocketData() {
		return this.socketData;
	}

	public void setSocketData(Socket socketData) {
		this.socketData = socketData;
	}

	public static class ConnectionWriter {

		private PrintWriter out;

		public ConnectionWriter(PrintWriter out) {
			this.out = out;
		}

		public void println(String line) {
			System.out.println("###################: " + line);
			out.println(line);
		}

		public void println(Command cmd) {
			this.println(cmd.toString());
		}
	}
}
