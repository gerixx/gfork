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

public class MyCallableTask extends CallableTask {

	private static final long serialVersionUID = 1L;
	
	private String text = "undef";

	@Override
	public Object getImplementingObject() {
		return this;
	}

	@Override
	public boolean isReadyToBeCalled() {
		return true;
	}
	
	public void set(String text) {
		this.text = text;
	}

	public String get() {
		return this.text;
	}

	@Override
	protected void initialize() throws Exception {
	}
}
