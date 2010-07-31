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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PipedTextTask extends LinkableAdapter implements Runnable {

	private static final long serialVersionUID = 1L;
	
	private final static Logger log = Logger.getLogger(PipedTextTask.class.getName());

	public void run() {
		final BufferedReader in = new BufferedReader(new InputStreamReader(this.predReadStream));
		final PrintWriter out = new PrintWriter(new OutputStreamWriter(this.succWriteStream));
		String line;
		try {
			log.info("start processing");
			while((line = in.readLine()) != null) {
				process(out, line);
			}
			log.info("end processing");
		} catch (final IOException e) {
			log.log(Level.SEVERE, "processing error", e);
		}
		out.close();
	}

	protected void process(final PrintWriter out, final String line) {
		out.println(line);
	}
}
