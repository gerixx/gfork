package org.gfork.internal.remote.server;

import java.net.Socket;

import org.gfork.internal.remote.Connection;

public class ConnectionServerSide extends Connection {

	public ConnectionServerSide(Socket socket) throws Exception {
		super(socket);
	}

	@Override
	protected String getConnectionLogPrefix() {
		return "connection server: ";
	}

}
