package org.gfork.internal.remote;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class Connection {

	public Socket socketControl;
	public Socket socketData;
	public PrintWriter socketControlWriter;
	public Scanner socketControlScanner;
	public String id;

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

}
