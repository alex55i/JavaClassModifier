package ru.alex55i.jclassmodifier;

import javassist.CtClass;

public interface ClassContainer
{
	public String getSimpleName();

	public byte[] getBytes() throws Exception;

	public void updateClass(CtClass clazz) throws Exception;
}
