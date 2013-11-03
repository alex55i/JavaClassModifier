package ru.alex55i.jclassmodifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class Config extends Properties
{
	private static final long serialVersionUID = 1L;

	public static final File configFile = new File(System.getProperty("user.home"), "classmodifier.conf");

	public synchronized void load()
	{
		try
		{
			FileInputStream is = new FileInputStream(configFile);
			load(is);
		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private synchronized void store()
	{
		try
		{
			FileOutputStream os = new FileOutputStream(configFile);
			store(os, null);
		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	@Override
	public synchronized String getProperty(String key, String defaultValue)
	{
		String s = super.getProperty(key);
		if (s == null)
		{
			setProperty(key, defaultValue);
			store();
			return defaultValue;
		}
		return s;
	}

	@Override
	public synchronized Object setProperty(String key, String value)
	{
		Object o = super.setProperty(key, value);
		store();
		return o;
	}

	public File getFile(String key)
	{
		String v = getProperty(key);
		return v != null ? new File(v) : null;
	}

	public void setFile(String key, File f)
	{
		setProperty(key, f.getAbsolutePath());
	}

}
