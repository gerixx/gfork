package org.gfork.internal.remote.client;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.gfork.internal.remote.Connection;

public class ConnectionClientSide extends Connection {

	public ConnectionClientSide(Socket socket, String id) throws Exception {
		super(socket, id);
	}

	public ConnectionClientSide(InetSocketAddress serverAddress, String id) throws Exception {
		super(serverAddress, id);
	}

	@Override
	protected String getConnectionLogPrefix() {
		return "connection client: ";
	}

}
