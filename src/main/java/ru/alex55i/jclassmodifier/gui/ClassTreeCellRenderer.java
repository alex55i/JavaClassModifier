package ru.alex55i.jclassmodifier.gui;

import java.awt.Color;
import java.awt.Component;
import java.io.File;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;

import ru.alex55i.jclassmodifier.gui.tab.ClassFileTab;
import ru.alex55i.jclassmodifier.tree.ClassFileTabNode;
import ru.alex55i.jclassmodifier.tree.ClasspathNode;
import ru.alex55i.jclassmodifier.tree.ZipEntryNode;
import ru.alex55i.jclassmodifier.tree.ZipFileNode;

public class ClassTreeCellRenderer extends DefaultTreeCellRenderer
{
	private static final long serialVersionUID = 1L;

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
	{
		String stringValue = value.toString();
		Color fg = Color.black;
		if (value instanceof ClasspathNode)
		{
			stringValue = ((ClasspathNode) value).getFile().getName();
			fg = Color.blue;
		}
		else if (value instanceof ClassFileTabNode)
		{
			ClassFileTabNode node = (ClassFileTabNode) value;
			ClassFileTab tab = node.getTab();
			stringValue = node.toString();
			if (tab == null)
			{
				fg = Color.gray;
			} else
			{
				if (tab.isChanged())
					stringValue = "* " + stringValue;
			}
		}
		else if (value instanceof ZipFileNode)
		{
			File zipFile = ((ZipFileNode) value).getFile();
			stringValue = zipFile.getName();
		}
		else if (value instanceof ZipEntryNode)
		{
			ZipEntryNode node = (ZipEntryNode) value;
			ClassFileTab tab = node.getTab();
			stringValue = node.toString();
			if (tab == null)
			{
				fg = Color.gray;
			} else
			{
				if (tab.isChanged())
					stringValue = "* " + stringValue;
			}
		}

		this.hasFocus = hasFocus;
		setText(stringValue);

		setForeground(fg);

		Icon icon = null;
		if (leaf)
		{
			icon = getLeafIcon();
		} else if (expanded)
		{
			icon = getOpenIcon();
		} else
		{
			icon = getClosedIcon();
		}

		if (!tree.isEnabled())
		{
			setEnabled(false);
			LookAndFeel laf = UIManager.getLookAndFeel();
			Icon disabledIcon = laf.getDisabledIcon(tree, icon);
			if (disabledIcon != null)
				icon = disabledIcon;
			setDisabledIcon(icon);
		} else
		{
			setEnabled(true);
			setIcon(icon);
		}
		setComponentOrientation(tree.getComponentOrientation());

		selected = sel;
		return this;
	}
}
