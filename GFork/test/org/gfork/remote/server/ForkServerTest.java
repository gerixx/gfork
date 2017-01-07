package org.gfork.remote.server;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.Socket;

import org.gfork.internal.remote.Command;
import org.gfork.internal.remote.Connection;
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
		Connection con = new Connection(socket, "id-1");
		con.socketControlWriter.println(Command.connect);
		con.socketControlWriter.println("id-1");

		try (Socket dataSocket = new Socket(InetAddress.getLocalHost(), ForkServer.DEFAULT_PORT)) {
			dataSocket.getOutputStream().write("id-1".getBytes());
			dataSocket.getOutputStream().flush();
		}

		String connectReply = con.socketControlScanner.nextLine();
		assertEquals("id-1", connectReply);
		connectReply = con.socketControlScanner.nextLine();
		assertEquals(Command.connectOK.toString(), connectReply);
	}

	public static void startLocalDefaultForkServer() {
		Thread forkServerThread = new Thread("ForkServerTest") {
			public void run() {
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
	}

}
