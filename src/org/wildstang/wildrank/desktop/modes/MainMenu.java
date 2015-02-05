package org.wildstang.wildrank.desktop.modes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileSystemView;

public class MainMenu extends Mode implements ActionListener {
	JButton getEventData;
	JButton generateMatchCSV;
	JButton generatePitCSV;
	JButton generatePDF;
	JButton setFlashLocation;
	JButton setLocalLocation;
	JButton compileNotes;
	JButton saveConfiguration;
	JButton addTeamsManually;
	JButton addMatchManually;
	JButton clearDirectories;
	JButton noteAdder;
	JButton initialSync;
	JButton sync;
	JButton newConfig;
	JLabel about;
	JLabel aboutb;

	@Override
	protected void initializePanel() {
		getEventData = new JButton("Get Event Data");
		getEventData.addActionListener(MainMenu.this);
		generateMatchCSV = new JButton("Generate CSV");
		generateMatchCSV.addActionListener(MainMenu.this);
		generatePitCSV = new JButton("Prepare Pit Data");
		generatePitCSV.addActionListener(MainMenu.this);
		generatePDF = new JButton("Generate PDFs");
		generatePDF.addActionListener(MainMenu.this);
		setFlashLocation = new JButton("Set Local Tablet Flash Scratch Location");
		setFlashLocation.addActionListener(MainMenu.this);
		setLocalLocation = new JButton("Set Local Location for the Entire Event");
		setLocalLocation.addActionListener(MainMenu.this);
		saveConfiguration = new JButton("Save Configuration");
		saveConfiguration.addActionListener(MainMenu.this);
		compileNotes = new JButton("Prepare Notes");
		compileNotes.addActionListener(MainMenu.this);
		newConfig = new JButton("New Config File");
		newConfig.addActionListener(MainMenu.this);
		initialSync = new JButton("Set Up Tablet Initially");
		initialSync.addActionListener(this);
		sync = new JButton("Sync with the Tablet Drive");
		sync.addActionListener(this);
		noteAdder = new JButton("Add Notes Manually");
		noteAdder.addActionListener(this);
		clearDirectories = new JButton("Clear Directories");
		clearDirectories.addActionListener(this);
		addTeamsManually = new JButton("Add Teams Manually");
		addTeamsManually.addActionListener(this);
		addMatchManually = new JButton("Add Matches Manually");
		addMatchManually.addActionListener(this);
		about = new JLabel(appData.getFlashDriveLocation().toString());
		about.setHorizontalAlignment(SwingConstants.CENTER);
		aboutb = new JLabel(" " + appData.getLocalLocation().toString());
		aboutb.setHorizontalAlignment(SwingConstants.CENTER);
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		panel.add(sync, c);
		c.gridwidth = 1;
		c.gridy = 1;
		panel.add(getEventData, c);
		c.gridx = 1;
		panel.add(generatePitCSV, c);
		c.gridx = 0;
		c.gridy = 2;
		panel.add(generateMatchCSV, c);
		c.gridx = 1;
		panel.add(generatePDF, c);
		c.gridx = 0;
		c.gridy = 3;
		panel.add(compileNotes, c);
		c.gridx = 1;
		panel.add(addTeamsManually, c);
		c.gridx = 0;
		c.gridy = 4;
		panel.add(addMatchManually, c);
		c.gridx = 1;
		panel.add(noteAdder, c);
		c.gridx = 0;
		c.gridy = 5;
		panel.add(setFlashLocation, c);
		c.gridx = 1;
		panel.add(setLocalLocation, c);
		c.gridx = 0;
		c.gridy = 6;
		panel.add(about, c);
		c.gridx = 1;
		panel.add(aboutb, c);
		c.gridx = 0;
		c.gridy = 7;
		c.gridwidth = 2;
		panel.add(saveConfiguration, c);
		c.gridy = 8;
		panel.add(clearDirectories, c);
		c.gridy = 9;
		panel.add(newConfig, c);
		update.setMode("Main Menu");
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == generateMatchCSV) {
			setMode(new MatchCSVGenerator());
		} else if (event.getSource() == generatePitCSV) {
			setMode(new PitCSVGenerator());
		} else if (event.getSource() == compileNotes) {
			setMode(new NoteCompiler());
		} else if (event.getSource() == setFlashLocation) {
			appData.setFlashDriveLocation(getFlashDriveLocation());
			about.setText(appData.getFlashDriveLocation().toString());
		} else if (event.getSource() == setLocalLocation) {
			appData.setLocalLocation(getLocalLocation());
			aboutb.setText(appData.getLocalLocation().toString());
		} else if (event.getSource() == getEventData) {
			setMode(new EventSelector());
		} else if (event.getSource() == saveConfiguration) {
			appData.save();
		} else if (event.getSource() == initialSync) {
			// Put code here for initial sync with tablet
			// TODO: Put code here for initial sync with tablet
		} else if (event.getSource() == sync) {
			setMode(new SyncWithFlashDrive());
		} else if (event.getSource() == noteAdder) {
			setMode(new ManualNoteEntering());
		} else if (event.getSource() == generatePDF) {
			setMode(new PDFWriter());
		} else if (event.getSource() == addTeamsManually) {
			setMode(new ManualTeamEntering());
		} else if (event.getSource() == addMatchManually) {
			setMode(new ManualMatchEntering());
		} else if (event.getSource() == clearDirectories) {
			setMode(new ClearDirectories());
		} else if (event.getSource() == newConfig) {
			setMode(new ConfigCreator());
		}
	}

	public static File getLocalLocation() {
		JFileChooser chooser = new JFileChooser();
		File startFile = new File(System.getProperty("user.home"));
		chooser.setCurrentDirectory(chooser.getFileSystemView().getParentDirectory(startFile));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("Select the Local location");
		if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
			return chooser.getSelectedFile();
		} else {
			return null;
		}
	}

	public static File getFlashDriveLocation() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		// Get the current user directory
		File startFile = new File(System.getProperty("user.dir"));

		// Find System Root
		while (!FileSystemView.getFileSystemView().isFileSystemRoot(startFile)) {
			startFile = startFile.getParentFile();
		}

		chooser.setCurrentDirectory(chooser.getFileSystemView().getParentDirectory(startFile));
		chooser.setDialogTitle("Select the Flash Drive location");
		if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
			return chooser.getSelectedFile();
		} else {
			return null;
		}
	}
}
