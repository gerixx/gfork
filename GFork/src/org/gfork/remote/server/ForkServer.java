package org.gfork.remote.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gfork.internal.remote.Command;
import org.gfork.internal.remote.Connection;
import org.gfork.internal.remote.ReplyData;

public class ForkServer {

	public static int DEFAULT_PORT = 54165;

	private static int MAX_FORKS_RUNNING = 5;

	private static final int SOCKET_READ_TIMEOUT = 9000;

	private static final String JAVA_UTIL_LOGGING_SIMPLE_FORMATTER_FORMAT = "java.util.logging.SimpleFormatter.format";

	private static final Logger LOG = Logger.getLogger(ForkServer.class.getName());

	private static final String ARG_PORT = "-port";

	private static Map<String, Object> options = new HashMap<>();
	private static boolean stop;
	private static ServerSocket serverSocket;
	private static Map<String, Connection> connections = new HashMap<>();

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
		Connection con = null;
		try {
			ReplyData acceptReply = ForkServer.readReply(Command.connect, socket.getInputStream());
			if (acceptReply.isExpectedToken()) {
				con = new Connection(socket);
				if (connections.size() > MAX_FORKS_RUNNING) {
					con.replyMsgAndClose("ERROR: maximum nuber " + MAX_FORKS_RUNNING + " of forks exceeded.");
					return;
				}

				LOG.info("wait for data connect for connection ID '" + con.id + "' ...");
				con.socketData = serverSocket.accept();
				ReplyData dataReply = ForkServer.readReply(con.id, con.socketData.getInputStream());
				if (!dataReply.isExpectedToken()) {
					final String msg = "ERROR: invalid data connection, expected ID '" + con.id + "', got '"
							+ dataReply.getReplyToken() + "'" + (dataReply.isTimedOut() ? ", read timed out" : "")
							+ ", handshake aborted.";
					LOG.severe(msg);
					con.replyMsgAndClose(msg);
				} else {
					LOG.info("successfully created connection for ID '" + con.id + "'");
					connections.put(con.id, con);
					con.socketControlWriter.println(con.id);
					con.socketControlWriter.println(Command.connectOK);
				}
			} else {
				String errorMsg = "ERROR: invalid connect";
				LOG.severe(errorMsg);
				socket.getOutputStream().write(errorMsg.getBytes());
				socket.getOutputStream().flush();
				Connection.closeSocket(socket);
			}
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "handShake", e);
			if (con != null) {
				con.replyMsgAndClose(e.toString());
			}
		}
	}

	// private static void readID(Connection con) throws IOException {
	// byte[] idBytes = new byte[con.ID.getBytes().length];
	// con.socketData.getInputStream().read(idBytes, 0,
	// con.ID.getBytes().length);
	// }

	private static ReplyData readReply(Command cmd, InputStream inputStream) throws Exception {
		return readReply(cmd.toString(), inputStream);
	}

	private static ReplyData readReply(String token, InputStream inputStream) throws Exception {
		int toread = token.getBytes().length;
		byte[] buff = new byte[toread];
		int len = 0, red = 0;
		long endTime = SOCKET_READ_TIMEOUT + System.currentTimeMillis();
		boolean isTimedOut = false;
		do {
			if (inputStream.available() == 0) {
				Thread.sleep(300);
			} else {
				len = inputStream.read(buff, red, toread - red);
				red += len;
			}
		} while (red < toread && !(isTimedOut = endTime < System.currentTimeMillis()));
		if (isTimedOut) {
			LOG.warning(() -> "reading from input stream timed out");
			return new ReplyData(false, true, (red > 0 ? new String(buff, 0, red) : ""));
		} else {
			String replyToken = new String(buff);
			return new ReplyData(token.equals(replyToken), false, replyToken);
		}
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

	private static String readStdInLine(String prompt) {
		System.out.print(prompt);
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		try {
			String line = reader.readLine();
			reader.close();
			return line;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static void stop() throws Exception {
		stop = true;
		if (!serverSocket.isClosed()) {
			LOG.info("server listener will be closed now");
			serverSocket.close();
		}
	}
}
