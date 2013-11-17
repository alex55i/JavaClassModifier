package ru.alex55i.jclassmodifier.gui;

import it.sauronsoftware.junique.MessageHandler;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.apache.commons.lang.exception.ExceptionUtils;

import ru.alex55i.jclassmodifier.ClassContainer;
import ru.alex55i.jclassmodifier.Helper;
import ru.alex55i.jclassmodifier.classcontainers.FileClassContainer;
import ru.alex55i.jclassmodifier.decompiler.Decompiler;
import ru.alex55i.jclassmodifier.decompiler.FernflowerDecompiler;
import ru.alex55i.jclassmodifier.gui.tab.ClassFileTab;
import ru.alex55i.jclassmodifier.mod.DecompiledFile;
import ru.alex55i.jclassmodifier.tree.ClassFileTabNode;
import ru.alex55i.jclassmodifier.tree.ClasspathNode;
import ru.alex55i.jclassmodifier.tree.ZipEntryNode;
import ru.alex55i.jclassmodifier.tree.ZipFileNode;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class EditorPanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	private final Decompiler decompiler = new FernflowerDecompiler();
	private JTabbedPane tabbedPane;
	private JButton outDirectoryBox;
	private JToolBar toolBar;
	private File outDirectory;
	private JTree classTree;
	private final AtomicInteger decompileCounter = new AtomicInteger();
	private JLabel decompileCounterLabel;

	public EditorPanel()
	{
		setLayout(new BorderLayout());

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		classTree = new JTree(new DefaultMutableTreeNode());
		classTree.setRootVisible(false);
		classTree.expandRow(0);
		classTree.setCellRenderer(new ClassTreeCellRenderer());
		classTree.addTreeSelectionListener(getTreeSelectionListener());
		JPanel leftPane = new JPanel(new BorderLayout());
		JScrollPane scroll = new JScrollPane(classTree);
		leftPane.add(scroll);
		JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton addClasspath = new JButton("Add Classpath");
		addClasspath.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				File currentDir = MainFrame.getConfig().getFile("add-classpath");
				if (currentDir == null)
					currentDir = new File(".");
				JFileChooser chooser = new JFileChooser(currentDir);
				chooser.setPreferredSize(new Dimension(700, 600));
				chooser.setMultiSelectionEnabled(true);
				int res = chooser.showOpenDialog(null);
				MainFrame.getConfig().setFile("add-classpath", chooser.getCurrentDirectory());
				if (res == JFileChooser.APPROVE_OPTION)
				{
					final File[] files = chooser.getSelectedFiles();
					for (File f : files)
						addClasspath(f);
					reloadTreeModel();
				}
			}
		});

		JButton removeClasspath = new JButton("Remove");
		removeClasspath.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				DefaultTreeSelectionModel selModel = (DefaultTreeSelectionModel) classTree.getSelectionModel();
				TreePath[] pathes = selModel.getSelectionPaths();
				if (pathes != null)
				{
					for (TreePath path : pathes)
					{
						Object obj = path.getLastPathComponent();
						if (obj != null)
						{
							if (obj instanceof ClassFileTabNode)
							{
								ClassFileTabNode node = (ClassFileTabNode) obj;
								removeAndCloseClassFileTab(node);
							}
							else if (obj instanceof ClasspathNode)
							{
								ClasspathNode node = (ClasspathNode) obj;
								removeClassPathNode(node);
							}
							else if (obj instanceof ZipFileNode)
							{
								ZipFileNode node = (ZipFileNode) obj;
								removeZipNodeAndCloseTabs(node);
							}
							reloadTreeModel();
						}
					}
				}
			}
		});

		controls.add(addClasspath);
		controls.add(removeClasspath);
		leftPane.add(controls, "North");
		split.add(leftPane);

		tabbedPane = new JTabbedPane();
		split.add(tabbedPane);
		add(split);

		toolBar = buildToolbar();
		add(toolBar, "North");

		add(buildStatusBar(), "South");

		MainFrame.setHandler(new MessageHandler()
		{
			@Override
			public String handle(String arg0)
			{
				openZipOrClassFileNodes(new File(arg0));
				reloadTreeModel();
				return null;
			}
		});
		for (String s : MainFrame.getProgramArgs())
		{
			openZipOrClassFileNodes(new File(s));
		}
		reloadTreeModel();
	}

	private Component buildStatusBar()
	{
		JPanel bar = new JPanel();
		bar.setLayout(new BoxLayout(bar, BoxLayout.X_AXIS));

		bar.add(new JLabel("Decompiling: "));

		decompileCounterLabel = new JLabel();
		bar.add(decompileCounterLabel);
		updateDecompileCounterLabel();

		return bar;
	}

	private void updateDecompileCounterLabel()
	{
		decompileCounterLabel.setText("" + decompileCounter.get());
	}

	private TreeSelectionListener getTreeSelectionListener()
	{
		return new TreeSelectionListener()
		{
			@Override
			public void valueChanged(TreeSelectionEvent e)
			{
				Object o = classTree.getLastSelectedPathComponent();
				if (o != null)
				{
					if (o instanceof ClassFileTabNode)
					{
						final ClassFileTabNode node = (ClassFileTabNode) o;
						final ClassContainer container = node.getContainer();
						ClassFileTab tab = node.getTab();
						if (tab == null)
						{
							new Thread()
							{
								public void run()
								{
									try
									{
										decompileCounter.incrementAndGet();
										SwingUtilities.invokeLater(new Runnable()
										{
											@Override
											public void run()
											{
												updateDecompileCounterLabel();
											}
										});
										final DecompiledFile source = Helper.decompile(decompiler, getClasspath(), container);
										decompileCounter.decrementAndGet();
										SwingUtilities.invokeLater(new Runnable()
										{
											@Override
											public void run()
											{
												ClassFileTab tab = new ClassFileTab(source);
												node.setTab(tab);
												classTree.repaint();
												openCloseableTab(node.toString(), tab);
											}
										});
									} catch (Exception e)
									{
										decompileCounter.decrementAndGet();
										openErrorTab(e, "Decompile error");
										e.printStackTrace();
									}
									SwingUtilities.invokeLater(new Runnable()
									{
										@Override
										public void run()
										{
											updateDecompileCounterLabel();
										}
									});
								};
							}.start();
						}
						else
						{
							openCloseableTab(node.toString(), tab);
						}
					}
					else if (o instanceof ZipEntryNode)
					{
						final ZipEntryNode node = (ZipEntryNode) o;
						final ClassContainer container = node.getContainer();
						ClassFileTab tab = node.getTab();
						if (tab == null)
						{
							new Thread()
							{
								public void run()
								{
									try
									{
										decompileCounter.incrementAndGet();
										SwingUtilities.invokeLater(new Runnable()
										{
											@Override
											public void run()
											{
												updateDecompileCounterLabel();
											}
										});
										final DecompiledFile source = Helper.decompile(decompiler, getClasspath(), container);
										decompileCounter.decrementAndGet();
										SwingUtilities.invokeLater(new Runnable()
										{
											@Override
											public void run()
											{
												ClassFileTab tab = new ClassFileTab(source);
												node.setTab(tab);
												classTree.repaint();
												openCloseableTab(node.toString(), tab);
											}
										});
									} catch (Exception e)
									{
										decompileCounter.decrementAndGet();
										openErrorTab(e, "Decompile error");
										e.printStackTrace();
									}
									SwingUtilities.invokeLater(new Runnable()
									{
										@Override
										public void run()
										{
											updateDecompileCounterLabel();
										}
									});
								};
							}.start();
						}
						else
						{
							openCloseableTab(node.toString(), tab);
						}
					}
				}
			}
		};
	}

	protected void reloadTreeModel()
	{
		final DefaultTreeModel model = (DefaultTreeModel) classTree.getModel();
		final DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();

		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				TreePath nodesPath = new TreePath(root.getPath());
				TreePath currentSel = classTree.getLeadSelectionPath();
				Enumeration<TreePath> expandEnum = classTree.getExpandedDescendants(nodesPath);
				model.reload(root);
				if (expandEnum != null)
				{
					while (expandEnum.hasMoreElements())
						classTree.expandPath(expandEnum.nextElement());
				}
				classTree.setSelectionPath(currentSel);
			}
		};
		if (SwingUtilities.isEventDispatchThread())
		{
			r.run();
		}
		else
		{
			SwingUtilities.invokeLater(r);
		}
	}

	protected void addClasspath(File f)
	{
		TreeModel model = classTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		root.add(new ClasspathNode(f));
	}

	protected Collection<File> getClasspath()
	{
		ArrayList<File> list = new ArrayList<File>();
		TreeModel model = classTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		Enumeration<?> children = root.children();
		while (children.hasMoreElements())
		{
			Object o = children.nextElement();
			if (o instanceof ClasspathNode)
			{
				list.add(((ClasspathNode) o).getFile());
			}
		}
		return list;
	}

	protected Collection<ClassFileTab> getChangedTabs()
	{
		TreeModel model = classTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		return getChangedTabs(root);
	}

	protected Collection<ClassFileTab> getChangedTabs(DefaultMutableTreeNode root)
	{
		ArrayList<ClassFileTab> list = new ArrayList<ClassFileTab>();
		Enumeration<?> children = root.children();
		while (children.hasMoreElements())
		{
			Object o = children.nextElement();
			if (o instanceof ClassFileTabNode)
			{
				ClassFileTabNode node = (ClassFileTabNode) o;
				ClassFileTab tab = node.getTab();
				if (tab != null && tab.isChanged())
				{
					list.add(tab);
				}
			}
			else if (o instanceof ZipEntryNode)
			{
				ZipEntryNode node = (ZipEntryNode) o;
				ClassFileTab tab = node.getTab();
				if (tab != null && tab.isChanged())
				{
					list.add(tab);
				}
			}
			else if (o instanceof DefaultMutableTreeNode)
			{
				list.addAll(getChangedTabs((DefaultMutableTreeNode) o));
			}
		}
		return list;
	}

	protected void addClassFile(File classFile)
	{
		TreeModel model = classTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		root.add(new ClassFileTabNode(new FileClassContainer(classFile), null));
	}

	protected void addZipFileNode(File file) throws ZipException, IOException
	{
		TreeModel model = classTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		root.add(new ZipFileNode(file));
	}

	protected void removeAndCloseClassFileTab(ClassFileTabNode node)
	{
		TreeModel model = classTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		root.remove(node);

		Component tab = node.getTab();
		if (tab != null)
			tabbedPane.remove(tab);
	}

	protected void removeClassPathNode(ClasspathNode node)
	{
		TreeModel model = classTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		root.remove(node);
	}

	protected void removeZipNodeAndCloseTabs(ZipFileNode node)
	{
		TreeModel model = classTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		root.remove(node);
		Enumeration<?> children = node.children();
		while (children.hasMoreElements())
		{
			Object o = children.nextElement();
			if (o instanceof ZipEntryNode)
			{
				ClassFileTab tab = ((ZipEntryNode) o).getTab();
				if (tab != null)
					tabbedPane.remove(tab);
			}
		}
	}

	protected void openZipOrClassFileNodes(File file)
	{
		String filename = file.getName();
		if (filename.endsWith(".zip") || filename.endsWith(".jar"))
		{
			try
			{
				addZipFileNode(file);
			} catch (Exception e)
			{
				e.printStackTrace();
				openErrorTab(e, "Error opening file");
			}
		}
		else if (filename.endsWith(".class"))
		{
			addClassFile(file);
		}
	}

	private JToolBar buildToolbar()
	{
		JToolBar bar = new JToolBar(JToolBar.HORIZONTAL);
		JButton el = new JButton("Open class files...");
		el.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				File currentDir = MainFrame.getConfig().getFile("open-classfile");
				if (currentDir == null)
					currentDir = new File(".");
				JFileChooser chooser = new JFileChooser(currentDir);
				chooser.setPreferredSize(new Dimension(700, 600));
				chooser.setMultiSelectionEnabled(true);
				int res = chooser.showOpenDialog(null);
				MainFrame.getConfig().setFile("open-classfile", chooser.getCurrentDirectory());
				if (res == JFileChooser.APPROVE_OPTION)
				{
					final File[] files = chooser.getSelectedFiles();
					new Thread()
					{
						@Override
						public void run()
						{
							for (File file : files)
								openZipOrClassFileNodes(file);
							reloadTreeModel();
						}
					}.start();
				}
			}
		});
		bar.add(el);
		el = new JButton("Save source...");
		el.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Component comp = tabbedPane.getSelectedComponent();
				if (comp != null && comp instanceof ClassFileTab)
				{
					File currentDir = MainFrame.getConfig().getFile("save-source");
					if (currentDir == null)
						currentDir = new File(".");
					JFileChooser chooser = new JFileChooser(currentDir);
					chooser.setPreferredSize(new Dimension(700, 600));
					int res = chooser.showSaveDialog(null);
					MainFrame.getConfig().setFile("save-source", chooser.getCurrentDirectory());
					if (res == JFileChooser.APPROVE_OPTION)
					{
						File f = chooser.getSelectedFile();
						ClassFileTab tab = (ClassFileTab) comp;
						try
						{
							Files.write(tab.getText(), f, Charsets.UTF_8);
						} catch (IOException ex)
						{
							ex.printStackTrace();
						}
					}
				}
			}
		});
		bar.add(el);

		bar.addSeparator();

		outDirectoryBox = new JButton();
		setOutputDir(MainFrame.getConfig().getFile("compile-directory"));
		outDirectoryBox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser c = new JFileChooser(outDirectory);
				c.setPreferredSize(new Dimension(700, 600));
				c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int res = c.showSaveDialog(null);
				if (res == JFileChooser.APPROVE_OPTION)
				{
					File f = c.getSelectedFile();
					setOutputDir(f);
					MainFrame.getConfig().setFile("compile-directory", f);
				}
			}
		});
		bar.add(outDirectoryBox);

		el = new JButton("Compile Changed");
		el.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Thread t = new Thread()
				{
					@Override
					public void run()
					{
						try
						{
							Collection<ClassFileTab> tabs = getChangedTabs();
							for (ClassFileTab tab : tabs)
							{
								DecompiledFile cf = tab.constructClassFile();
								Helper.compileAndUpdate(getClasspath(), cf);
								tab.setUnchanged();
							}
						} catch (Exception e)
						{
							e.printStackTrace();
							openErrorTab(e, "Compile error");
						}
					}
				};
				t.start();
			}
		});
		bar.add(el);

		bar.addSeparator();

		el = new JButton("Close All Tabs");
		el.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				tabbedPane.removeAll();
			}
		});
		bar.add(el);
		return bar;
	}

	public void setOutputDir(File dir)
	{
		if (dir != null && dir.isDirectory())
		{
			outDirectory = dir;
			outDirectoryBox.setText("Output dir: " + dir.getName());
		}
		else
		{
			outDirectory = null;
			outDirectoryBox.setText("Choose output directory...");
		}
	}

	private void openErrorTab(Throwable t, String title)
	{
		JTextPane pane = new JTextPane();
		pane.setEditable(false);
		pane.setForeground(Color.red);
		pane.setText(ExceptionUtils.getFullStackTrace(t));
		openCloseableTab(title, pane);
	}

	private void openCloseableTab(final String title, final Component c)
	{
		tabbedPane.addTab(null, c);
		int pos = tabbedPane.indexOfComponent(c);
		JPanel pnlTab = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
		pnlTab.setOpaque(false);

		JLabel lblTitle = new JLabel(title);
		final JButton btnClose = new JButton(" X ");
		btnClose.setOpaque(false);

		btnClose.setBorder(null);
		btnClose.setFocusable(false);
		pnlTab.add(lblTitle);
		pnlTab.add(btnClose);
		pnlTab.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

		tabbedPane.setTabComponentAt(pos, pnlTab);

		tabbedPane.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				btnClose.setEnabled(c == ((JTabbedPane) e.getSource()).getSelectedComponent());
			}
		});

		ActionListener listener = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				tabbedPane.remove(c);
			}
		};
		btnClose.addActionListener(listener);

		tabbedPane.setSelectedComponent(c);
	}
}
