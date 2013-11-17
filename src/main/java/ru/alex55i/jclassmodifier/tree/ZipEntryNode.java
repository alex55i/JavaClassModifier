package ru.alex55i.jclassmodifier.tree;

import ru.alex55i.jclassmodifier.classcontainers.ZipClassContainer;
import ru.alex55i.jclassmodifier.gui.CustomTreeNode;
import ru.alex55i.jclassmodifier.gui.tab.ClassFileTab;

public class ZipEntryNode extends CustomTreeNode
{
	private static final long serialVersionUID = 1L;
	private ZipClassContainer container;
	private ClassFileTab tab;

	public ZipEntryNode(ZipClassContainer container, ClassFileTab tab)
	{
		this.container = container;
		this.tab = tab;
	}

	public ZipClassContainer getContainer()
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
