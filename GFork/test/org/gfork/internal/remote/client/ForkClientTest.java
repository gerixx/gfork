package org.gfork.internal.remote.client;

import org.gfork.remote.server.ForkServer;
import org.gfork.remote.server.ForkServerTest;
import org.junit.After;
import org.junit.Test;

public class ForkClientTest {

	@After
	public void endTest() throws Exception {
		ForkServer.stop();
	}

	@Test
	public void testConnectToServer() throws Exception {
		ForkServerTest.startLocalDefaultForkServer();

		ForkClient client = ForkClient.connect("localhost");
		client.connect();
		client.close();
	}

}
