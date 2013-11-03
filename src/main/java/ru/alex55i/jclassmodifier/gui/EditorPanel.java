package ru.alex55i.jclassmodifier.gui;

import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javassist.ClassPool;
import javassist.CtClass;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;

import org.apache.commons.lang.exception.ExceptionUtils;

import ru.alex55i.jclassmodifier.decompiler.Decompiler;
import ru.alex55i.jclassmodifier.decompiler.FernflowerDecompiler;
import ru.alex55i.jclassmodifier.gui.tab.ClassFileTab;
import ru.alex55i.jclassmodifier.mod.JavaClassFile;
import ru.alex55i.jclassmodifier.modifier.ClassModifier;
import ru.alex55i.jclassmodifier.modifier.JavassistModifier;
import ru.alex55i.jclassmodifier.util.UrlClassPath;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class EditorPanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	private Decompiler defaultDecompiler;
	private JTabbedPane tabbedPane;
	private JButton outDirectoryBox;
	private JToolBar toolBar;
	private File outDirectory;
	private JTree classTree;
	private List<ClassFileTab> tabs = new ArrayList<ClassFileTab>();
	private List<File> classpath = new ArrayList<File>();

	public EditorPanel()
	{
		setLayout(new BorderLayout());

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		classTree = new JTree(new DefaultMutableTreeNode());
		classTree.setRootVisible(false);
		classTree.setCellRenderer(new DefaultTreeCellRenderer()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
			{
				String stringValue = value.toString();
				Color fg = null;
				if (value instanceof DefaultMutableTreeNode)
				{
					Object obj = ((DefaultMutableTreeNode) value).getUserObject();
					if (obj instanceof File)
					{
						fg = Color.blue;
						stringValue = ((File) obj).getName();
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
		});
		classTree.addTreeSelectionListener(new TreeSelectionListener()
		{
			@Override
			public void valueChanged(TreeSelectionEvent e)
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) classTree.getLastSelectedPathComponent();
				if (node != null)
				{
					Object obj = node.getUserObject();
					if (obj instanceof ClassFileTab)
					{
						ClassFileTab tab = (ClassFileTab) obj;
						openCloseableTab(node.toString(), tab);
					}
				}
			}
		});
		JPanel leftPane = new JPanel(new BorderLayout());
		JScrollPane scroll = new JScrollPane(classTree);
		leftPane.add(scroll);
		JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton addClasspath = new JButton("Add Classpath");
		addClasspath.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				File file = MainFrame.getConfig().getFile("add-classpath");
				if (file == null)
					file = new File(".");
				JFileChooser chooser = new JFileChooser(file);
				chooser.setPreferredSize(new Dimension(700, 600));
				chooser.setMultiSelectionEnabled(true);
				int res = chooser.showOpenDialog(null);
				MainFrame.getConfig().setFile("add-classpath", chooser.getCurrentDirectory());
				if (res == JFileChooser.APPROVE_OPTION)
				{
					final File[] files = chooser.getSelectedFiles();
					for (File f : files)
						classpath.add(f);
					rebuildTree();
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
						DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
						if (node != null)
						{
							Object obj = node.getUserObject();
							if (obj instanceof ClassFileTab)
							{
								ClassFileTab tab = (ClassFileTab) obj;
								tabs.remove(tab);
								tabbedPane.remove(tab);
							}
							else if (obj instanceof File)
							{
								File f = (File) obj;
								classpath.remove(f);
							}
						}
					}
					rebuildTree();
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

		defaultDecompiler = new FernflowerDecompiler();
	}

	protected void rebuildTree()
	{
		DefaultTreeModel model = (DefaultTreeModel) classTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		root.removeAllChildren();

		for (ClassFileTab tab : tabs)
		{
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(tab);
			root.add(node);
		}
		for (File f : classpath)
		{
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(f);
			root.add(node);
		}
		model.reload(root);
	}

	private JToolBar buildToolbar()
	{
		JToolBar bar = new JToolBar(JToolBar.HORIZONTAL);
		JButton el = new JButton("Open class files...");
		el.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				File file = MainFrame.getConfig().getFile("open-classfile");
				if (file == null)
					file = new File(".");
				JFileChooser chooser = new JFileChooser(file);
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
							decompile(files);
							SwingUtilities.invokeLater(new Runnable()
							{

								@Override
								public void run()
								{
									rebuildTree();
								}
							});
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
					File file = MainFrame.getConfig().getFile("save-source");
					if (file == null)
						file = new File(".");
					JFileChooser chooser = new JFileChooser(file);
					chooser.setPreferredSize(new Dimension(700, 600));
					int res = chooser.showSaveDialog(null);
					MainFrame.getConfig().setFile("save-source", chooser.getCurrentDirectory());
					if (res == JFileChooser.APPROVE_OPTION)
					{
						File f = chooser.getSelectedFile();
						saveSelectedToFile(f, (ClassFileTab) comp);
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

		el = new JButton("Compile All");
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
							JavaClassFile[] classFiles = new JavaClassFile[tabs.size()];
							int i = 0;
							for (ClassFileTab tab : tabs)
							{
								classFiles[i++] = tab.getClassFile();
							}
							compile(classFiles);
						} catch (ParseException e)
						{
							e.printStackTrace();
						}
					}
				};
				t.start();
			}
		});
		bar.add(el);
		return bar;
	}

	public void decompile(final File[] classFiles)
	{
		try
		{
			System.out.println("Decompiling..");
			CompilationUnit[] units = defaultDecompiler.decompileClassFiles(classpath.toArray(new File[classpath.size()]), classFiles);
			System.out.println("Decompiled!");
			if (units.length > 0)
			{
				System.out.println("Opening " + units.length + " tabs");
				for (final CompilationUnit unit : units)
				{
					final JavaClassFile srcClass = new JavaClassFile(unit);
					srcClass.recomputeChecksum();
					SwingUtilities.invokeAndWait(new Runnable()
					{
						public void run()
						{
							ClassFileTab tab = new ClassFileTab(srcClass);
							tabs.add(tab);
						}
					});
				}
			}
			else
			{
				System.err.println("No class files found!");
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			openErrorTab(e, "Decompile error");
		}
	}

	public void compile(final JavaClassFile[] classFiles)
	{
		if (outDirectory == null)
		{
			JOptionPane.showMessageDialog(null, "Choose output dir first.");
			return;
		}
		try
		{
			ClassPool pool = new ClassPool(true);
			for (File cf : classpath)
			{
				pool.appendClassPath(new UrlClassPath(cf.toURI().toURL()));
			}
			System.out.println("Compiling");
			CompilationUnit[] units = new CompilationUnit[classFiles.length];
			for (int i = 0; i < classFiles.length; i++)
			{
				units[i] = classFiles[i].getUnit();
			}
			ClassModifier modifer = new JavassistModifier(pool, units);
			CtClass[] outClasses = modifer.modify();
			System.out.println("Changed classes:");
			for (CtClass c : outClasses)
			{
				// Save changed classes
				String path = c.getName().replace('.', '/') + ".class";
				File file = new File(outDirectory, path);
				file.getParentFile().mkdirs();
				DataOutputStream os = new DataOutputStream(new FileOutputStream(file));
				try
				{
					c.toBytecode(os);
				} finally
				{
					os.flush();
					os.close();
				}

				System.out.println(c.getName());
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			openErrorTab(e, "Error compiling");
		}
	}

	public void saveSelectedToFile(File file, ClassFileTab tab)
	{
		try
		{
			Files.write(tab.getText(), file, Charsets.UTF_8);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
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
