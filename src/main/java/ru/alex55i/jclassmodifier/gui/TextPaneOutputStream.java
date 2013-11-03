package ru.alex55i.jclassmodifier.gui;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

public class TextPaneOutputStream extends OutputStream
{
	private JTextPane textPane;
	private Style style;

	public TextPaneOutputStream(JTextPane textPane, boolean error)
	{
		this.textPane = textPane;
		if (error)
		{
			style = textPane.addStyle("Error", null);
			StyleConstants.setForeground(style, Color.red);
		}
	}

	@Override
	public void write(int b) throws IOException
	{
		append(String.valueOf((char) b));
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		append(new String(b, off, len));
	}

	private void append(final String text)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				Document doc = textPane.getDocument();
				try
				{
					doc.insertString(doc.getLength(), text, style);
				} catch (BadLocationException e)
				{}
				textPane.setCaretPosition(doc.getLength() - 1);
			}
		});
	}

}
