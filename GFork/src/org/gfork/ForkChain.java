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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;


/**
 * Use {@link ForkChain} to conveniently link {@link ForkLink} objects together. Every
 * {@link ForkLink} object represents a single link of a {@link Fork} chain. 
 * Linking with {@link ForkChain} means the
 * task output of a Fork will be forwarded to the next Fork task in the same way as pipes do.
 * 
 * @author Gerald Ehmayer
 *
 */
@SuppressWarnings("unchecked")
public class ForkChain {

	private final ForkLink[] chain;
	private boolean executing;
	private ServerSocket endListener;
	private Socket beginSocket;
	private Socket endSocket;
	private boolean inputRetrieved;

	/**
	 * Constructs a Fork chain.
	 * 
	 * @param chain array of Fork elements that will be tied together
	 */
	public ForkChain(final ForkLink... chain) {
		if (chain == null) {
			throw new IllegalArgumentException("Null not allowed for parameter chain.");
		}
		this.chain = chain;
	}
	
	public ForkChain(final Collection<ForkLink> chain) {
		this((ForkLink[]) chain.toArray(new ForkLink[chain.size()]));
	}
	
	public synchronized void execute() throws Exception {
		if (executing) {
			throw new IllegalStateException("Fork chain is already executing.");
		}
		executing = true;
		
		// create listener for data output at the end of the chain (= output of the last fork)
		endListener = new ServerSocket(0);
		int port = endListener.getLocalPort();
		if (port <= 0) {
			throw new RuntimeException(String.format("Invalid listener port created: %d", port));
		}
		
		// bottom up boot of chain forks
		for (int i = chain.length-1; i >= 0; i--) {
			ForkLink fork = chain[i];
			fork.setSuccessorPort(port); // where fork writes to
			fork.execute();
			port = fork.readForkListenerPort(); 
		}
		
		// connect port for data input at the begin of the chain (= input for the first fork)
		beginSocket = new Socket("127.0.0.1", port);
	}
	
	/**
	 * Retrieves data input of the chain.
	 * 
	 * @return input stream of the first chain's {@link ForkLink} object
	 * @throws IOException 
	 * @throws IllegalStateException the output stream to read from the chain was already retrieved
	 */
	public synchronized OutputStream getBeginWriteStream() throws IOException {
		if (inputRetrieved) {
			throw new IllegalStateException("Output stream to read from chain cannot be retrieved more than once.");
		}
		inputRetrieved = true;
		return beginSocket.getOutputStream();
	}
	
	/**
	 * Retrieves InputStream to read data output of the chain.
	 * 
	 * @return output stream of the last chain's {@link ForkLink} object
	 * @throws IOException 
	 * @throws IllegalStateException the input stream to write to the chain was already retrieved
	 */
	public synchronized InputStream getEndReadStream() throws IOException {
		if (endSocket != null) {
			throw new IllegalStateException("Input stream to write to chain cannot be retrieved more than once.");
		}
		endSocket = endListener.accept();
		endListener.close();
		endListener = null;
		return endSocket.getInputStream();
	}

	public synchronized void close() throws IOException {
		if (beginSocket != null) {
			beginSocket.close();
			beginSocket = null;
		}
		if (endSocket != null) {
			endSocket.close();
			endSocket = null;
		}
	}

	public boolean isExecuting() {
		return executing;
	}
	
	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}
	
}
