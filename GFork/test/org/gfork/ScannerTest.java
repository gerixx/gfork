package org.gfork;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

/**
 * Java 8 or later needed.
 * 
 */
public class ScannerTest {

	private static final int READER_BLOCKING_SECONDS = 5;
	
	private static final String LINE_TEXT = "some text";
	private static final String dummyText = LINE_TEXT + System.getProperty("line.separator")
		+ LINE_TEXT + System.getProperty("line.separator")
		+ LINE_TEXT + System.getProperty("line.separator")
		+ LINE_TEXT + System.getProperty("line.separator")
		;

	private static class ReaderBlockingRead extends StringReader {


		public ReaderBlockingRead() {
			super(dummyText);
		}

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			try {
				System.out.println("reading is blocked");
				Thread.sleep(READER_BLOCKING_SECONDS * 1000); // 5 seconds is enough for our test
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 

			return super.read(cbuf, off, len);
		}
	}

	/**
	 * Basic test of helper {@link ReaderBlockingRead}.
	 */
	@Test
	public void testReaderBlockingFirstRead() {
		Readable source = new ReaderBlockingRead();
		Scanner scanner = new Scanner(source);

		long beforeRead = System.currentTimeMillis();
		String line = scanner.nextLine();
		assertEquals(LINE_TEXT, line);
		assertTrue(System.currentTimeMillis() - beforeRead > (READER_BLOCKING_SECONDS * 1000));

		scanner.close();
	}

	/**
	 * Test abortable usage of {@link Scanner#nextLine()} together with a {@link FutureTask}.
	 */
	@Test(expected = TimeoutException.class)
	public void testReadNextLineWithTimeout() throws Exception {
		Readable source = new ReaderBlockingRead();
		
		try (Scanner scanner = new Scanner(source)) { // ensure to close scanner to avoid resource leak
			readNextLineWithTimeout(scanner, READER_BLOCKING_SECONDS / 2);
		}
	}

	private String readNextLineWithTimeout(Scanner scanner, int timeoutSec) throws InterruptedException, ExecutionException, TimeoutException {
		FutureTask<String> readNextLine = new FutureTask<String>(() -> {
			return scanner.nextLine();
		});
		ExecutorService executor = Executors.newFixedThreadPool(1); // shutdown needed in production
		executor.execute(readNextLine);
		return readNextLine.get(timeoutSec, TimeUnit.SECONDS);
	}
}
