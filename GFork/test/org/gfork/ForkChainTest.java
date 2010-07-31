/*
   Copyright 2010 Gerald Ehmayer
   
   This file is part of project GFork.

    GFork is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GFork is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with GFork.  If not, see <http://www.gnu.org/licenses/>.

*/

package org.gfork;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.gfork.Fork;
import org.gfork.ForkChain;
import org.gfork.ForkLink;
import org.gfork.helpers.PipedTextTask;
import org.gfork.tasks.AppenderTask;
import org.gfork.types.Void;
import org.junit.Before;
import org.junit.Test;


public class ForkChainTest {

	@Before
	public void init() {
		// configure logging before every test because some of the tests disable it
		Fork.setLoggingEnabled(true);
		Fork.setJvmOptionsForAll(new String[] {
				"-Djava.util.logging.config.file=./logging_forkRunner.properties",
		});
	}

	@Test
	public void testSingleLinkChain() throws Exception {
		PipedTextTask task = new PipedTextTask();
		ForkLink<PipedTextTask, Void> fork = new ForkLink<PipedTextTask, Void>(task);
		
		ForkChain chain = new ForkChain(fork);
		chain.execute();
		
		final OutputStream chainInput = chain.getBeginWriteStream();
		final InputStream chainOutput = chain.getEndReadStream();
		
		// write data input to chain
		Thread writer = new Thread() {
			@Override
			public void run() {
				PrintWriter out = new PrintWriter(new OutputStreamWriter(chainInput));
				out.println("line-1");
				out.println("line-2");
				out.println("line-3");
				out.close();
			}
		};
		writer.start();
		
		// read data output from chain
		ArrayList<String> taskOutResult = new ArrayList<String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(chainOutput));
		String line;
		while((line = in.readLine()) != null) {
			taskOutResult.add(line);
		}
		
		// check output
		assertEquals(3, taskOutResult.size());
		int cnt = 1;
		for (String outLine : taskOutResult) {
			assertEquals("line-" + cnt, outLine);
			cnt++;
		}
	}
	
	@Test
	public void testShortChain() throws Exception {
		AppenderTask task = new AppenderTask();
		ForkLink<AppenderTask, Void> fork1 = new ForkLink<AppenderTask, Void>(task);
		ForkLink<AppenderTask, Void> fork2 = new ForkLink<AppenderTask, Void>(task);
		
		ForkChain chain = new ForkChain(fork1, fork2);
		chain.execute();
		
		final OutputStream chainInput = chain.getBeginWriteStream();
		final InputStream chainOutput = chain.getEndReadStream();
		
		// write data input to chain
		Thread writer = new Thread() {
			@Override
			public void run() {
				PrintWriter out = new PrintWriter(new OutputStreamWriter(chainInput));
				out.println("line");
				out.println("line");
				out.close();
			}
		};
		writer.start();
		
		// read data output from chain
		ArrayList<String> taskOutResult = new ArrayList<String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(chainOutput));
		String line;
		while((line = in.readLine()) != null) {
			taskOutResult.add(line);
		}
		
		// check output
		assertEquals(2, taskOutResult.size());
		for (String outLine : taskOutResult) {
			assertEquals("line_A_A", outLine);
		}
	}
	
	@Test
	public void testLongChain() throws Exception {
		AppenderTask task = new AppenderTask();
		ForkLink<AppenderTask, Void> fork1 = new ForkLink<AppenderTask, Void>(task);
		ForkLink<AppenderTask, Void> fork2 = new ForkLink<AppenderTask, Void>(task);
		ForkLink<AppenderTask, Void> fork3 = new ForkLink<AppenderTask, Void>(task);
		ForkLink<AppenderTask, Void> fork4 = new ForkLink<AppenderTask, Void>(task);
		ForkLink<AppenderTask, Void> fork5 = new ForkLink<AppenderTask, Void>(task);
		ForkLink<AppenderTask, Void> fork6 = new ForkLink<AppenderTask, Void>(task);
		ForkLink<AppenderTask, Void> fork7 = new ForkLink<AppenderTask, Void>(task);
		ForkLink<AppenderTask, Void> fork8 = new ForkLink<AppenderTask, Void>(task);
		ForkLink<AppenderTask, Void> fork9 = new ForkLink<AppenderTask, Void>(task);
		
		ForkChain chain = new ForkChain(fork1, fork2, fork3, fork4, fork5, fork6, fork7, fork8, fork9);
		chain.execute();
		
		final OutputStream chainInput = chain.getBeginWriteStream();
		final InputStream chainOutput = chain.getEndReadStream();
		
		// write data input to chain
		Thread writer = new Thread() {
			@Override
			public void run() {
				PrintWriter out = new PrintWriter(new OutputStreamWriter(chainInput));
				out.println("line1");
				out.println("line2");
				out.close();
			}
		};
		writer.start();
		
		// read data output from chain
		ArrayList<String> taskOutResult = new ArrayList<String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(chainOutput));
		String line;
		while((line = in.readLine()) != null) {
			taskOutResult.add(line);
		}
		
		// check output
		assertEquals(2, taskOutResult.size());
		int cnt = 1;
		for (String outLine : taskOutResult) {
			assertEquals("line" + cnt + "_A_A_A_A_A_A_A_A_A", outLine);
			cnt++;
		}
	}
	
}
