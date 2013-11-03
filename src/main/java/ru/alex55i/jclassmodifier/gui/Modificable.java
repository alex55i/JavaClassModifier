package ru.alex55i.jclassmodifier.gui;

import java.io.File;

import ru.alex55i.jclassmodifier.decompiler.Decompiler;
import ru.alex55i.jclassmodifier.mod.JavaClassFile;


public class Modificable
{
	public File[] originalClasses;
	public Decompiler decompiler;
	public JavaClassFile source;

	public Modificable(File[] originalClasses, Decompiler decompiler, JavaClassFile source)
	{
		this.originalClasses = originalClasses;
		this.decompiler = decompiler;
		this.source = source;
	}

}
