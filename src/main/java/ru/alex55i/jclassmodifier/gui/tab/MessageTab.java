package ru.alex55i.jclassmodifier.gui.tab;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JTextPane;

public class MessageTab extends JPanel
{
	private static final long serialVersionUID = 1L;

	public MessageTab(boolean error, String message)
	{
		setLayout(new BorderLayout());

		JTextPane pane = new JTextPane();
		pane.setForeground(error ? Color.red : Color.black);
		pane.setText(message);
		add(pane);
	}
}
