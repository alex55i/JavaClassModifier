package ru.alex55i.jclassmodifier.tree;

import java.io.File;

import ru.alex55i.jclassmodifier.gui.CustomTreeNode;

public class ClasspathNode extends CustomTreeNode
{
	private static final long serialVersionUID = 1L;
	private File file;

	public ClasspathNode(File file)
	{
		this.file = file;
	}

	public File getFile()
	{
		return file;
	}
}
