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

package org.gfork.helpers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PipedStreamTask extends LinkableAdapter {

	private static final long serialVersionUID = 1L;

	private final static Logger log = Logger.getLogger(PipedStreamTask.class.getName());
	
	private int outBufferSize = 5000;
	private int inBufferSize = 5000;
	private int maxProcessBytes = 1024;

	public void run() {
		final BufferedInputStream in = new BufferedInputStream(this.predReadStream, inBufferSize);
		final BufferedOutputStream out = new BufferedOutputStream(this.succWriteStream, outBufferSize);
		int len;
		byte[] b = new byte[maxProcessBytes];
		try {
			log.info("start processing");
			while((len = in.read(b)) > 0) {
				process(out, b, len);
			}
			log.info("end processing");
			out.close();
		} catch (final IOException e) {
			log.log(Level.SEVERE, "processing error", e);
		}
	}

	protected void process(BufferedOutputStream out, byte[] b, int len) throws IOException {
		out.write(b, 0, len);
	}

	public synchronized void setOutBufferSize(int outBufferSize) {
		this.outBufferSize = outBufferSize;
	}

	public synchronized void setInBufferSize(int inBufferSize) {
		this.inBufferSize = inBufferSize;
	}

	public synchronized void setMaxProcessBytes(int maxProcessBytes) {
		this.maxProcessBytes = maxProcessBytes;
	}

}
