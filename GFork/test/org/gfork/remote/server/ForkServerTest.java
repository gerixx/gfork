package org.gfork.remote.server;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.Socket;

import org.gfork.internal.remote.Command;
import org.gfork.internal.remote.client.ConnectionClientSide;
import org.junit.After;
import org.junit.Test;

public class ForkServerTest {

	@After
	public void endTest() throws Exception {
		ForkServer.stop();
	}

	@Test
	public void testConnect() throws Exception {
		startLocalDefaultForkServer();

		Socket socket = new Socket(InetAddress.getLocalHost(), ForkServer.DEFAULT_PORT);
		ConnectionClientSide con = new ConnectionClientSide(socket, "id-1");
		con.getSocketControlWriter().println(Command.connect);
		con.getSocketControlWriter().println("id-1");

		try (Socket dataSocket = new Socket(InetAddress.getLocalHost(), ForkServer.DEFAULT_PORT)) {
			dataSocket.getOutputStream().write("id-1".getBytes());
			dataSocket.getOutputStream().flush();
		}

		String connectReply = con.getSocketControlScanner().nextLine();
		assertEquals("id-1", connectReply);
		connectReply = con.getSocketControlScanner().nextLine();
		assertEquals(Command.connectOk.toString(), connectReply);
	}

	public static Thread startLocalDefaultForkServer() {
		Thread forkServerThread = new Thread("ForkServerTest server thread") {
			public void run() {
				System.out.println("ForkServerTest: server thread starting...");
				ForkServer.main(new String[] {});
				System.out.println("ForkServerTest: server thread ended");
			}

			@Override
			public synchronized void start() {
				super.start();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		};
		forkServerThread.start();
		return forkServerThread;
	}

}
