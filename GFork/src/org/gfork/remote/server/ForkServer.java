package org.gfork.remote.server;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gfork.Fork;
import org.gfork.internal.remote.Command;
import org.gfork.internal.remote.ReplyData;
import org.gfork.internal.remote.client.ForkClient;
import org.gfork.internal.remote.server.ConnectionServerSide;
import org.gfork.internal.remote.server.ForkServerConnectionProcessor;

/**
 * Use it to start a ForkServer to execute remote Fork tasks, see {@link Fork#execute(String)}.
 * Default listening port is {@link ForkServer#DEFAULT_PORT}.
 * <p>
 * Example: 
 * <pre>
 *   $/&gt; java org.gfork.remote.server.ForkServer [-port 1234]
 * </pre>
 * 
 * @author Gerald Ehmayer
 *
 */
public class ForkServer {

	/**
	 * Default port: 54165
	 */
	public static int DEFAULT_PORT = 54165;

	/**
	 * Maximum number of concurrent running fork processes is 10.
	 */
	public static int MAX_FORKS_RUNNING = 10;

	public static final int SOCKET_READ_TIMEOU_IN_MILLIS = 9000;

	private static final String JAVA_UTIL_LOGGING_SIMPLE_FORMATTER_FORMAT = "java.util.logging.SimpleFormatter.format";

	public static final Logger LOG = Logger.getLogger(ForkServer.class.getName());

	private static final String ARG_PORT = "-port";

	private static Map<String, Object> options = new HashMap<>();
	private static boolean stop;
	private static ServerSocket serverSocket;
	private static Map<String, ForkServerConnectionProcessor> connections = new HashMap<>();

	static {
		if (null == System.getProperty(JAVA_UTIL_LOGGING_SIMPLE_FORMATTER_FORMAT)) {
			// see
			// https://docs.oracle.com/javase/7/docs/api/java/util/logging/SimpleFormatter.html#format(java.util.logging.LogRecord)
			System.setProperty(JAVA_UTIL_LOGGING_SIMPLE_FORMATTER_FORMAT, "[%1$tc] %4$.4s: %5$s - %2$s %6$s%n");
		}
		options.put(ARG_PORT, DEFAULT_PORT);
	}

	public static void main(String[] args) {
		try {
			parseArgs(args);
			openServerSocket();
			do {
				Socket socket = serverSocket.accept();
				LOG.info("accepted connect from " + socket.getRemoteSocketAddress());
				handleConnect(socket);
			} while (!stop);
			LOG.info(() -> "stopped");
		} catch (Exception e) {
			if (stop) {
				LOG.info("server listening loop ended");
			} else {
				LOG.log(Level.SEVERE, "server listening loop ended unexpected", e);
			}
		}
	}

	private static void handleConnect(final Socket socket) throws Exception {
		ConnectionServerSide con = null;
		try {
			ReplyData acceptReply = ForkServer.readReply(Command.connect, socket.getInputStream());
			if (acceptReply.isExpectedToken()) {
				con = new ConnectionServerSide(socket);
				if (connections.size() > MAX_FORKS_RUNNING) {
					con.replyMsgAndClose("ERROR: maximum nuber " + MAX_FORKS_RUNNING + " of forks exceeded.");
					return;
				}

				LOG.info("wait for data connect for connection ID '" + con.getId() + "' ...");
				con.setSocketData(serverSocket.accept());
				ReplyData dataReply = ForkClient.readReply(con.getId(), con.getSocketData().getInputStream());
				if (!dataReply.isExpectedToken()) {
					final String msg = "ERROR: invalid data connection, expected ID '" + con.getId() + "', got '"
							+ dataReply.getReplyToken() + "'" + (dataReply.isTimedOut() ? ", read timed out" : "")
							+ ", handshake aborted.";
					LOG.severe(msg);
					con.replyMsgAndClose(msg);
				} else {
					LOG.info("successfully created connection for ID '" + con.getId() + "'");
					con.getSocketControlWriter().println(con.getId());
					con.getSocketControlWriter().println(Command.connectOk);
					ForkServerConnectionProcessor processor = new ForkServerConnectionProcessor(con);
					processor.start(connections);
				}
			} else {
				String errorMsg = "ERROR: invalid connect";
				LOG.severe(errorMsg);
				socket.getOutputStream().write(errorMsg.getBytes());
				socket.getOutputStream().flush();
				ConnectionServerSide.closeSocket(socket);
			}
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "handShake", e);
			if (con != null) {
				con.replyMsgAndClose(e.toString());
			}
		}
	}

	// private static void readID(ConnectionServerSide con) throws IOException {
	// byte[] idBytes = new byte[con.getId().getBytes().length];
	// con.getSocketData().getInputStream().read(idBytes, 0,
	// con.getId().getBytes().length);
	// }

	private static ReplyData readReply(Command cmd, InputStream inputStream) throws Exception {
		return ForkClient.readReply(cmd.toString(), inputStream);
	}

	private static void openServerSocket() throws Exception {
		serverSocket = new ServerSocket((Integer) options.get(ARG_PORT));
	}

	private static void parseArgs(String[] args) {
		String argName = null;
		for (String arg : args) {
			if (argName.equals(ARG_PORT)) {
				options.put(ARG_PORT, Integer.parseInt(argName));
				argName = null;
				continue;
			}
			argName = arg;
		}
		LOG.info(() -> "listening port = " + options.get(ARG_PORT) + ", can be changed using argument '-port'");
	}

	public static void stop() throws Exception {
		stop = true;
		if (!serverSocket.isClosed()) {
			LOG.info("server listener will be closed now");
			serverSocket.close();
		}
	}
}
