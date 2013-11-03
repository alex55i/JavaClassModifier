package ru.alex55i.jclassmodifier.mod;

import javassist.CtConstructor;

public class ModConstructor
{
	CtConstructor constructor;
	String body;

	public ModConstructor(CtConstructor constructor, String body)
	{
		this.constructor = constructor;
		this.body = body;
	}

	public CtConstructor getConstructor()
	{
		return constructor;
	}

	public String getBody()
	{
		return body;
	}

}
