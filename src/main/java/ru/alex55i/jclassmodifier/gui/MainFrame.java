package ru.alex55i.jclassmodifier.gui;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import jsyntaxpane.DefaultSyntaxKit;
import ru.alex55i.jclassmodifier.Config;

public class MainFrame extends JFrame
{
	private static final long serialVersionUID = 1L;
	private static final Config config = new Config();

	public static void main(String[] args)
	{
		config.load();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				DefaultSyntaxKit.initKit();
				new MainFrame().setVisible(true);
			}
		});
	}

	public MainFrame()
	{
		setPreferredSize(new Dimension(1000, 700));
		setTitle("Java Class Modifier");
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setContentPane(new ContentPanel());

		pack();
		setLocationRelativeTo(null);
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});
	}

	public static Config getConfig()
	{
		return config;
	}

}
