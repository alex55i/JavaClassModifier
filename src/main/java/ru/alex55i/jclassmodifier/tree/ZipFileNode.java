package ru.alex55i.jclassmodifier.tree;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.tree.TreeNode;

import ru.alex55i.jclassmodifier.classcontainers.ZipClassContainer;
import ru.alex55i.jclassmodifier.gui.CustomTreeNode;

public class ZipFileNode extends CustomTreeNode
{
	private static final long serialVersionUID = 1L;
	private File file;

	public ZipFileNode(File file) throws ZipException, IOException
	{
		this.file = file;
		children = new Vector<TreeNode>();
		Enumeration<? extends ZipEntry> entries = new ZipFile(file).entries();
		List<String> pathes = new ArrayList<String>();
		ZipEntry entry;
		while (entries.hasMoreElements())
		{
			entry = entries.nextElement();
			if (!entry.isDirectory())
			{
				String name = entry.getName();
				if (name.endsWith(".class"))
				{
					pathes.add(name);
				}
			}
		}
		Collections.sort(pathes, new Comparator<String>()
		{
			@Override
			public int compare(String o1, String o2)
			{
				return getLastPart(o1).compareTo(getLastPart(o2));
			}

			String getLastPart(String path)
			{
				int ix = path.lastIndexOf('/');
				if (ix == -1)
					return path;
				else
					return path.substring(ix + 1);
			}
		});
		for (String path : pathes)
		{
			add(new ZipEntryNode(new ZipClassContainer(file, path), null));
		}
	}

	public File getFile()
	{
		return file;
	}
}
