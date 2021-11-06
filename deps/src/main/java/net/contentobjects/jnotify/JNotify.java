/*******************************************************************************
 * JNotify - Allow java applications to register to File system events.
 * 
 * Copyright (C) 2005 - Content Objects
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 ******************************************************************************
 * 
 * You may also redistribute and/or modify this library under the terms of the
 * Eclipse Public License. See epl.html.
 * 
 ******************************************************************************
 *
 * Content Objects, Inc., hereby disclaims all copyright interest in the
 * library `JNotify' (a Java library for file system events).
 * 
 * Yahali Sherman, 21 November 2005
 *    Content Objects, VP R&D.
 * 
 ******************************************************************************
 * Author : Omry Yadan
 ******************************************************************************/

package net.contentobjects.jnotify;

import java.io.File;
import java.io.IOException;



public class JNotify
{
	public static final int FILE_CREATED 	= 0x1;
	public static final int FILE_DELETED 	= 0x2;
	public static final int FILE_MODIFIED 	= 0x4;
	public static final int FILE_RENAMED 	= 0x8;
	public static final int FILE_ANY 		= FILE_CREATED | FILE_DELETED | FILE_MODIFIED | FILE_RENAMED;
	
	private static IJNotify _instance;
	
	static
	{
		String overrideClass = System.getProperty("jnotify.impl.override");
		if (overrideClass != null)
		{
			try
			{
				_instance = (IJNotify) Class.forName(overrideClass).newInstance();
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		else
		{
			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.equals("linux"))
			{
				try
				{
					_instance = (IJNotify) Class.forName("net.contentobjects.jnotify.linux.JNotifyAdapterLinux").newInstance();
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}
			else
			if (osName.startsWith("windows"))
			{
				try
				{
					_instance = (IJNotify) Class.forName("net.contentobjects.jnotify.win32.JNotifyAdapterWin32").newInstance();
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}
			else
			if (osName.startsWith("mac os x"))
			{
				try
				{
					_instance = (IJNotify) Class.forName("net.contentobjects.jnotify.macosx.JNotifyAdapterMacOSX").newInstance();
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}
	        else
			{
				throw new RuntimeException("Unsupported OS : " + osName);
			}
		}
	}
	
	public static int addWatch(String path, int mask, boolean watchSubtree, JNotifyListener listener) throws JNotifyException
	{
		return _instance.addWatch(path, mask, watchSubtree, listener);
	}

	public static boolean removeWatch(int watchId) throws JNotifyException
	{
		return _instance.removeWatch(watchId);
	}
	
	public static void main(String[] args) throws InterruptedException, IOException
	{
		String dir = new File(args.length == 0 ? "." : args[0]).getCanonicalFile().getAbsolutePath();
		JNotify.addWatch(dir, FILE_ANY, true, new JNotifyListener()
		{
			public void fileRenamed(int wd, String rootPath, String oldName,
					String newName)
			{
				System.out.println("renamed " + rootPath + " : " + oldName + " -> " + newName);
			}
			
			public void fileModified(int wd, String rootPath, String name)
			{
				System.out.println("modified " + rootPath + " : " + name);
			}
			
			public void fileDeleted(int wd, String rootPath, String name)
			{
				System.out.println("deleted " + rootPath + " : " + name);
			}
			
			public void fileCreated(int wd, String rootPath, String name)
			{
				System.out.println("created " + rootPath + " : " + name);
			}
		});
		
		System.out.println("Monitoring " + dir + ", ctrl+c to stop");
		while (true) Thread.sleep(10000);
	}
}
