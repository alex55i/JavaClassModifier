package ru.alex55i.jclassmodifier.classcontainers;

import java.io.File;

import javassist.CtClass;
import ru.alex55i.jclassmodifier.ClassContainer;

import com.google.common.io.Files;

public class FileClassContainer implements ClassContainer
{
	private File file;

	public FileClassContainer(File file)
	{
		this.file = file;
	}

	public String getSimpleName()
	{
		return file.getName();
	}

	@Override
	public void updateClass(CtClass clazz) throws Exception
	{
		Files.write(clazz.toBytecode(), file);
	}

	@Override
	public byte[] getBytes() throws Exception
	{
		return Files.toByteArray(file);
	}

	public File getFile()
	{
		return file;
	}
}
