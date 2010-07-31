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

package org.gfork.internal;

import static org.junit.Assert.*;

import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.Map.Entry;

import org.gfork.Fork;
import org.junit.BeforeClass;
import org.junit.Test;


public class SysPropTaskTest {

	@BeforeClass
	public static void staticSetUp() {
		Fork.setJvmOptionsForAll(new String[]{});
	}
	
	@Test
	public void testSubProcessSystemProperties() throws Exception {
		Fork<SysPropTask, Properties> f = new Fork<SysPropTask, Properties>(
				new SysPropTask(),
				SysPropTask.getSystemPropertiesMethod()
			);
		f.execute();
		Properties remoteSystemProperties = f.getReturnValue();
		Properties localSystemProperties = System.getProperties();
		assertEquals(localSystemProperties.size(), remoteSystemProperties.size());
		Set<Entry<Object, Object>> lProps = localSystemProperties.entrySet();
		for (Entry<Object, Object> lProp : lProps) {
			Object remotePropValue = remoteSystemProperties.get(lProp.getKey());
			Object localPropValue = lProp.getValue();
			if (lProp.getKey().equals("user.timezone")) {
				TimeZone localTimeZone = TimeZone.getTimeZone((String) localPropValue);
				TimeZone remoteTimeZone = TimeZone.getTimeZone((String) remotePropValue);
				assertEquals("key=" + lProp.getKey(), localTimeZone, remoteTimeZone);
			} else {
				assertEquals("key=" + lProp.getKey(), localPropValue, remotePropValue);
			}
		}
	}
}
	