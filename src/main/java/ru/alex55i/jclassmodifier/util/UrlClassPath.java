package ru.alex55i.jclassmodifier.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipException;

import javassist.ClassPath;

public class UrlClassPath implements ClassPath
{
	private String urlContext;
	private boolean isJarFile;

	public UrlClassPath(URL url) throws ZipException, IOException
	{
		String path = url.toString();
		isJarFile = path.endsWith(".jar") || path.endsWith(".zip");
		if (isJarFile)
			urlContext = "jar:" + path + "!/";
		else
			urlContext = path.endsWith("/") ? path : path + "/";
	}

	@Override
	public InputStream openClassfile(String classname)
	{
		try
		{
			URL url = find(classname);
			if (url == null)
				return null;
			return url.openStream();
		} catch (IOException e)
		{}
		return null;
	}

	@Override
	public URL find(String name)
	{
		try
		{
			URL url = new URL(urlContext + name.replace('.', '/') + ".class");
			url.openStream();
			return url;
		} catch (IOException e)
		{}
		return null;
	}

	@Override
	public void close()
	{}

}
