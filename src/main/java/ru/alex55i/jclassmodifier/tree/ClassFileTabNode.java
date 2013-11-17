package ru.alex55i.jclassmodifier.tree;

import ru.alex55i.jclassmodifier.classcontainers.FileClassContainer;
import ru.alex55i.jclassmodifier.gui.CustomTreeNode;
import ru.alex55i.jclassmodifier.gui.tab.ClassFileTab;

public class ClassFileTabNode extends CustomTreeNode
{
	private static final long serialVersionUID = 1L;
	private FileClassContainer container;
	private ClassFileTab tab;

	public ClassFileTabNode(FileClassContainer container, ClassFileTab tab)
	{
		this.container = container;
		this.tab = tab;
	}

	public FileClassContainer getContainer()
	{
		return container;
	}

	public void setTab(ClassFileTab tab)
	{
		this.tab = tab;
	}

	public ClassFileTab getTab()
	{
		return tab;
	}

	@Override
	public String toString()
	{
		return container.getSimpleName();
	}
}
