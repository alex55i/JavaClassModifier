package ru.alex55i.jclassmodifier.gui;

import java.awt.BorderLayout;
import java.io.PrintStream;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;

import org.apache.commons.io.output.TeeOutputStream;

public class ContentPanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	private JTabbedPane tabbed;
	private EditorPanel editorPanel;
	private JTextPane logPanel;

	public ContentPanel()
	{
		setLayout(new BorderLayout());

		tabbed = new JTabbedPane();
		editorPanel = new EditorPanel();
		tabbed.addTab("Editor", editorPanel);

		logPanel = new JTextPane();
		logPanel.setEditable(false);

		// Redirect streams
		System.setOut(new PrintStream(new TeeOutputStream(System.out, new TextPaneOutputStream(logPanel, false))));
		System.setErr(new PrintStream(new TeeOutputStream(System.err, new TextPaneOutputStream(logPanel, true))));
		JScrollPane scroll = new JScrollPane(logPanel);
		tabbed.addTab("Log", scroll);
		add(tabbed);
	}
	
}
