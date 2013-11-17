package ru.alex55i.jclassmodifier.classcontainers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javassist.CtClass;

import org.apache.commons.io.IOUtils;

import ru.alex55i.jclassmodifier.ClassContainer;

import com.google.common.io.ByteStreams;

public class ZipClassContainer implements ClassContainer
{
	private File file;
	private String path;

	public ZipClassContainer(File file, String path)
	{
		this.file = file;
		this.path = path;
	}

	@Override
	public String getSimpleName()
	{
		int ix = path.lastIndexOf('/');
		if (ix == -1)
			return path;
		else
			return path.substring(ix + 1);
	}

	@Override
	public void updateClass(CtClass clazz) throws Exception
	{
		byte[] data = clazz.toBytecode();
		ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
		Map<String, byte[]> map = new HashMap<String, byte[]>();
		try
		{
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null)
			{
				map.put(entry.getName(), IOUtils.toByteArray(zis));
			}
		} finally
		{
			if (zis != null)
				zis.close();
		}
		map.put(path, data);
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
		try
		{
			for (Map.Entry<String, byte[]> entry : map.entrySet())
			{
				zos.putNextEntry(new ZipEntry(entry.getKey()));
				zos.write(entry.getValue());
			}
			zos.flush();
		} finally
		{
			if (zos != null)
				zos.close();
		}
	}

	@Override
	public byte[] getBytes() throws Exception
	{
		byte[] bytecode = null;
		ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
		try
		{
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null)
			{
				if (entry.getName().equals(path))
				{
					bytecode = ByteStreams.toByteArray(zis);
				}
			}
		} finally
		{
			if (zis != null)
				zis.close();
		}
		return bytecode;
	}

	public File getFile()
	{
		return file;
	}

	public String getPath()
	{
		return path;
	}
}
