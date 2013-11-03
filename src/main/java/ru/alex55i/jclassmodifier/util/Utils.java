package ru.alex55i.jclassmodifier.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class Utils
{
	public final static void printProcessOutput(final Process p)
	{
		final BufferedReader ir = new BufferedReader(new InputStreamReader(p.getInputStream()));
		final BufferedReader er = new BufferedReader(new InputStreamReader(p.getErrorStream()));

		Thread t = new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					String line;
					while (null != (line = ir.readLine()))
					{
						System.out.println("[STDOUT]: " + line);
					}
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		};
		t.setDaemon(true);
		t.start();

		t = new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					String line;
					while (null != (line = er.readLine()))
					{
						System.err.println("[STDERR]: " + line);
					}
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}
}
