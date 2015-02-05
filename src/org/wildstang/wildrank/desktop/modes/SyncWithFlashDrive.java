package org.wildstang.wildrank.desktop.modes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import org.wildstang.wildrank.desktop.utils.FileUtilities;
import org.wildstang.wildrank.desktop.utils.Logger;

public class SyncWithFlashDrive extends Mode implements ActionListener, Runnable {

	JProgressBar progressBar;
	JButton sync;
	Thread thread;

	@Override
	protected void initializePanel() {
		progressBar = new JProgressBar();
		// add text to specify that they need to plug in the tablet now
		JLabel syncLabel = new JLabel("Connect the Tablet now to the PC first before clicking Sync below");
		sync = new JButton("Sync with the Tablet Now");
		sync.addActionListener(this);
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		panel.add(syncLabel, c);
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 3;
		panel.add(sync, c);
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 3;
		panel.add(progressBar, c);
		update.setMode("Sync");
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == sync) {
			attemptSyncronization();
		}
	}

	@Override
	public void run() {
		update.updateData("Syncing", 0, 0);
		progressBar.setIndeterminate(true);
		try {
			FileUtilities.syncWithTablet();

			// Now let the application sync what was just taken out of the tablet with the 
			// local folder
			FileUtilities.syncWithFlashDrive();
		} catch (IOException e) {
			e.printStackTrace();
		}
		update.updateData("Done syncing with flashdrive", 0, 0);
		progressBar.setIndeterminate(false);
		progressBar.setMaximum(1);
		progressBar.setValue(1);
		setMode(new MainMenu());
	}

	public void attemptSyncronization() {
		// Check if Tablet is available first
		if (FileUtilities.isTabletConnected()) {
			// Check local flash directory if it exist
			if (FileUtilities.isUSBConnected()) {
				Logger.getInstance().log("USB connected!");
				thread = new Thread(this);
				thread.start();
				sync.setEnabled(false);
			} else {
				Logger.getInstance().log("USB not connected!");
				JFrame frame = new JFrame();
				String[] options = { "Cancel", "Try again" };
				int choice = JOptionPane.showOptionDialog(frame, "Please connect a flash drive before trying to sync", "Connect Flash Drive", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
						options, options[0]);
				if (choice == 0) {
					setMode(new MainMenu());
				} else {
					attemptSyncronization();
				}
			}
		}
		else {
			JFrame frame = new JFrame();
			String[] options = { "Cancel", "Try again" };
			int choice = JOptionPane.showOptionDialog(frame, "Please connect a tablet before trying to sync", "Connect Tablet", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
					options, options[0]);
			if (choice == 0) {
				setMode(new MainMenu());
			} else {
				attemptSyncronization();
			}			
		}
	}
}
