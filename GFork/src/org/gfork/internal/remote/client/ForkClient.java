package org.gfork.internal.remote.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.gfork.Fork;
import org.gfork.internal.remote.Command;
import org.gfork.internal.remote.Connection;
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

	private int port = ForkServer.DEFAULT_PORT;

	private InetSocketAddress serverAddress;

	private static Map<String, ForkClient> forks = new HashMap<>();

	private Connection con;

	private boolean isRunning;

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

	protected void connect() throws Exception {
		con = new Connection(serverAddress, UUID.randomUUID().toString());
		handShake();
	}

	public void run(String className) throws IOException {
		if (isRunning) {
			throw new IllegalStateException(Fork.FORK_IS_ALREADY_EXECUTING);
		}
		con.socketControlWriter.println(className);
		con.socketControlWriter.println(Command.run);
		isRunning = true;
	}

	public void waitFor() {
		if (!isRunning) {
			throw new IllegalStateException(Fork.FORK_WAS_NOT_STARTED_YET);
		}
		// con.socketControlWriter.println(Command.waitFor);
		// con.socketControlScanner.nextLine();
	}

	public void close() {
		con.close();
	}

	private void handShake() throws Exception {
		con.socketControlWriter.println(Command.connect);
		con.socketControlWriter.println(con.id);

		try (Socket dataSocket = new Socket(InetAddress.getLocalHost(), ForkServer.DEFAULT_PORT)) {
			dataSocket.getOutputStream().write(con.id.getBytes());
			dataSocket.getOutputStream().flush();
		}

		String connectReply = con.socketControlScanner.nextLine();
		checkToken("ID response of server", con.id, connectReply);
		connectReply = con.socketControlScanner.nextLine();
		checkToken("ID response of server", Command.connectOK.toString(), connectReply);
	}

	public static void checkToken(String token, String idExpected, String idGot) {
		if (!idExpected.equals(idGot)) {
			throw new RuntimeException("Invalid " + token + ": expected=" + idExpected + ", got=" + idGot);
		}
	}

}
