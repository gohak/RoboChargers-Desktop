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

public class InitialSyncWithTablet extends Mode implements ActionListener, Runnable {

	JProgressBar progressBar;
	JButton initialSync;
	Thread thread;

	@Override
	protected void initializePanel() {
		progressBar = new JProgressBar();
		// add text to specify that they need to plug in the tablet now
        String html1 = "<html><body style='width: ";
        String html2 = "px'>";
		JLabel syncLabel = new JLabel(html1 + "500" + html2  + "This is the initial sync with the tablet that has no matches "
				+ "information on the tablet. If there is existing information, current workaround, "
				+ "delete the Android application first and then redownload it. Then connect the "
				+ "Tablet to the PC first before clicking Sync below. And then follow the instructions "
				+ "on the screen as it progresses.");
		initialSync = new JButton("Prepare the Tablet Now");
		initialSync.addActionListener(this);
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		panel.add(syncLabel, c);
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 3;
		panel.add(initialSync, c);
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 3;
		panel.add(progressBar, c);
		update.setMode("Sync");
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == initialSync) {
			attemptSyncronization();
		}
	}

	@Override
	public void run() {
		update.updateData("Setting up Tablet", 0, 0);
		progressBar.setIndeterminate(true);

		try {
			// Perform the initial sync only
			// TODO: need to create a FRC folder

			// Data Local --> Tablet
			FileUtilities.syncWithTablet(FileUtilities.INITIALSYNC);
		} catch (IOException e) {
			e.printStackTrace();
		}
		update.updateData("Done syncing with tablet", 0, 0);
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
				initialSync.setEnabled(false);
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
			int choice = JOptionPane.showOptionDialog(frame, "Please connect a tablet before performing the initial sync", "Connect Tablet", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
					options, options[0]);
			if (choice == 0) {
				setMode(new MainMenu());
			} else {
				attemptSyncronization();
			}			
		}
	}
}
