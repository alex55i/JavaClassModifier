package ru.alex55i.jclassmodifier.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.google.common.base.Joiner;

public class FileChooserBox extends JPanel
{
	private static final long serialVersionUID = 1L;
	private JTextField textField;
	private JButton findButton;
	private boolean save;
	private boolean multi;
	private File[] files;
	private int fileSelectionMode;

	public FileChooserBox(boolean save, boolean multi, int fileSelectionMode)
	{
		this.save = save;
		this.multi = multi;
		this.fileSelectionMode = fileSelectionMode;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		textField = new JTextField();
		textField.setEditable(false);
		add(textField);
		findButton = new JButton("Select...");
		findButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				showFileChooser();
			}
		});
		add(findButton);
	}

	protected void showFileChooser()
	{
		JFileChooser c = new JFileChooser(new File("."));
		c.setPreferredSize(new Dimension(700, 600));
		c.setFileSelectionMode(fileSelectionMode);
		if (save)
		{
			int res = c.showSaveDialog(null);
			if (res == JFileChooser.APPROVE_OPTION)
			{
				if (multi)
				{
					setFiles(c.getSelectedFiles());
				}
				else
				{
					setFile(c.getSelectedFile());
				}
			}
		}
		else
		{
			c.showOpenDialog(null);
		}
	}

	public void setFiles(File[] files)
	{
		this.files = files;
		String s = Joiner.on(", ").join(files);
		textField.setText(s);
	}

	public void setFile(File file)
	{
		setFiles(new File[] { file });
	}

	public File[] getFiles()
	{
		return files;
	}

	public File getFile()
	{
		return files.length < 1 ? null : files[0];
	}
}
