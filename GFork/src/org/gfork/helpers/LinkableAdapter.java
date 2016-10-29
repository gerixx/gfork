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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.gfork.Linkable;

public abstract class LinkableAdapter implements Linkable {

	private static final long serialVersionUID = 1L;
	
	protected transient InputStream predReadStream;
	protected transient OutputStream predWriteStream;
	
	protected transient OutputStream succWriteStream;
	protected transient InputStream succReadStream;

	@Override
	public void setPredTaskReadStream(InputStream in) throws IOException {
		this.predReadStream = in;
	}

	@Override
	public void setSuccTaskWriteStream(OutputStream out) throws IOException {
		this.succWriteStream = out;
	}

	@Override
	public void setPredTaskWriteStream(OutputStream out) throws IOException {
		this.predWriteStream = out;
	}

	@Override
	public void setSuccTaskReadStream(InputStream in) throws IOException {
		this.succReadStream = in;
	}

	@Override
	public void init(boolean isLoggingEnabled) throws IOException {
	}
	
}
