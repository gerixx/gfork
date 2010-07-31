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

package org.gfork.internal.types;

import java.io.Serializable;
import java.util.concurrent.Callable;

public class TaskDelegate<V> implements Serializable, Callable<V>, Runnable {

	public static final long serialVersionUID = 1L;
	private final Serializable callable;
	private final Serializable runnable;

	public <TASK_TYPE extends Serializable & Callable<V>> TaskDelegate(final TASK_TYPE callable) {
		this.callable = callable;
		this.runnable = null;
	}
	
	public <TASK_TYPE extends Serializable & Runnable> TaskDelegate(final TASK_TYPE runnable, final V result) {
		this.runnable = runnable;
		this.callable = null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public V call() throws Exception {
		return ((Callable<V>) callable).call();
	}

	@Override
	public void run() {
		((Runnable) runnable).run();
	}
};


