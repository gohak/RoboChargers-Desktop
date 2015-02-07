package org.wildstang.wildrank.desktop.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jmtp.PortableDevice;
import jmtp.PortableDeviceManager;
import jmtp.PortableDeviceObject;
import jmtp.PortableDeviceStorageObject;

import org.apache.commons.io.FileUtils;
import org.wildstang.wildrank.desktop.GlobalAppHandler;

import be.derycke.pieter.com.COMException;
import jmtp.*;

public class FileUtilities {
	public static final int INITIALSYNC = 0;
	public static final int RESYNC = 1;

	public static boolean isTabletConnected() {

		// Check if you have the DLL and notify user
		loadLib("", "jmtp.dll"); 
	
		System.setProperty( "java.library.path", "libs" );

        PortableDeviceManager manager = new PortableDeviceManager();

        for (PortableDevice device : manager.getDevices()) {
            // Connect to the tablet now
        	device.open();
            // debug output
            Logger.getInstance().log("Tablet Found - " + device.getModel());
            device.close();
            
            // Found a tablet connected
            return true;
        } 
		return false;
	}
	
	public static boolean isUSBConnected() {
		// Test if USB is connected
		String flashDriveSyncedDirectoryString = GlobalAppHandler.getInstance().getAppData().getFlashDriveLocation() + File.separator + "synced";
		File flashDriveSyncedDirectory = new File(flashDriveSyncedDirectoryString);
		flashDriveSyncedDirectory.mkdir();
		if (flashDriveSyncedDirectory.exists()) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isSavedConfigFilePresent() {
		return new File("save.json").exists();
	}

	public static void syncWithTablet(int mode) throws IOException {
        Logger.getInstance().log("Attempting to Sync with Tablet");

		// Copy all the content from the local Tablet Flash scratch directory to Tablet
		if (syncLocalFlashToTablet()) {

			// Now wait for user input. The user would need to now sync the tablet via "Synchronize with PC" on the Tablet
			if (waitForTabletToSync(mode) == 1) {
				// Now it copies all the data out of the tablet back to the local flash location
				syncTabletToLocalFlash();
			}
			
			// Now delete all the content from the tablet
			// There should only be one copy maintained which is on your local Flash drive
			// or in our case, we are using the PC
			cleanTabletData();
		}
		else {
            // if frc folder is not found, prompt user
	        frcFolderNotFound();
		}
	}
	
	public static boolean syncLocalFlashToTablet() throws IOException {
		System.setProperty( "java.library.path", "libs" );

		boolean frcFolderFound = false;
		
        PortableDeviceManager manager = new PortableDeviceManager();
        
        // Get the first device on the list.
        for (PortableDevice device : manager.getDevices()) {
	        // Connect to the tablet now
	        device.open();
	        
	        // debug output
	        Logger.getInstance().log("Copying data from Local Flash Scratch to Tablet - " + device.getModel());

	        // Iterate over deviceObjects
	        for(PortableDeviceObject object : device.getRootObjects()) {
	       	
	            // If the object is a storage object
	            if(object instanceof PortableDeviceStorageObject) {
	                PortableDeviceStorageObject storage = (PortableDeviceStorageObject)object;
	                
	                for(PortableDeviceObject o2 :  storage.getChildObjects()) {
	                	System.out.println(o2.getOriginalFileName());
	                	
	                	// Check if the device object is a folder first
	                	if (o2 instanceof PortableDeviceFolderObject) {
	                		// Check if there is a frc folder
    	                    if (o2.getOriginalFileName().equals("frc")) {
    	                    	frcFolderFound = true;
    	                    	
    	                    	// Now list all the content inside this frc folder object
    	                    	PortableDeviceFolderObject frcFolder = (PortableDeviceFolderObject)o2;
    	                		File relativeFrcFolder = new File(GlobalAppHandler.getInstance().getAppData().getFlashDriveLocation() + File.separator);
    	                		copyLocalFlashToTablet(device, frcFolder, relativeFrcFolder);
    	                    }
	                	}
	                }   
	            }
	        }
	        device.close();
        }
        // Return the status if frc folder was successfully synced.
        return frcFolderFound;
	}
	
	public static int frcFolderNotFound() {
		JFrame frame = new JFrame();
		String[] options = { "Cancel"};
		int choice = JOptionPane.showOptionDialog(frame, "This must be a new tablet as FRC folder is not found. Please create a \"frc\" folder on the root of the Tablet Internal Storage. Then try again.", "frc Folder Not Found", JOptionPane.CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, null,
				options, options[0]);
		return choice;
	}

	public static void copyLocalFlashToTablet(PortableDevice device, PortableDeviceFolderObject dir, File flashFolder) {
		// Obtain the list of folders and files here
		File[] files = flashFolder.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isFile() && !file.isHidden()) {
					// If it is a file, copy the file
					Logger.getInstance().log("copyLocalFlashToTablet; file:               " + file.getName());
					BigInteger a = new BigInteger("12345");
					try {
						dir.addAudioObject(file,"artist", "album", a);
					} catch (IOException e) {
						
					}
				} else if (file.isDirectory()) {
					Logger.getInstance().log("copyLocalFlashToTablet; directory: " + file.getAbsolutePath());
					PortableDeviceFolderObject dirI = dir;
					boolean createFolder = true;
					
					// Check if the folder exist first
					if (dir.getChildObjects() != null) {
						for(PortableDeviceObject folders :  dir.getChildObjects()) {
				        	if (folders instanceof PortableDeviceFolderObject) {
								PortableDeviceFolderObject folder = (PortableDeviceFolderObject)folders;
								// Create the directory if the folder does not exist
								if (folder.getOriginalFileName().equals(file.getName())) {
									createFolder = false;
									dirI = folder;
									
									// break out of the for loop early
									break;
								}
				        	}
						}
					}

					if (createFolder) {
						Logger.getInstance().log("copyLocalFlashToTablet; creating directory - " + file.getName());

						// Create the directory
						dirI = dir.createFolderObject(file.getName());
					}
					// Iterate through
					copyLocalFlashToTablet(device, dirI, file);
				}
			}
		}
	}

	public static void syncTabletToLocalFlash() throws IOException {		
        PortableDeviceManager manager = new PortableDeviceManager();
        
        // Get the first device on the list.
        for (PortableDevice device : manager.getDevices()) {
	        // Connect to the tablet now
	        device.open();
	        
	        // debug output
	        Logger.getInstance().log("Syncing with " + device.getModel());
	        System.out.println("---------------");
	
	        // Iterate over deviceObjects
	        for(PortableDeviceObject object : device.getRootObjects()) {
	        	System.out.println(object);
	       	
	            // If the object is a storage object
	            if(object instanceof PortableDeviceStorageObject) {
	                PortableDeviceStorageObject storage = (PortableDeviceStorageObject)object;
	                
	                for(PortableDeviceObject o2 :  storage.getChildObjects()) {
	                	System.out.println(o2.getOriginalFileName());
	                	
	                	// Check if the device object is a folder first
	                	if (o2 instanceof PortableDeviceFolderObject) {
	                		// Check if there is a frc folder
    	                    if (o2.getOriginalFileName().equals("frc")) {
    	                    	
    	                    	// Now list all the content inside this frc folder object
    	                    	PortableDeviceFolderObject frcFolder = (PortableDeviceFolderObject)o2;
    	                		File relativeFrcFolder = new File(GlobalAppHandler.getInstance().getAppData().getFlashDriveLocation() + File.separator);
    	                		copyTabletToLocalFlash(device, frcFolder, relativeFrcFolder);
    	                    }	                		
	                	}
	            	}              
	            }
	        }
	        device.close();
        }
	}
		
	public static int waitForTabletToSync(int mode) {
		JFrame frame = new JFrame();
		String[] options = { "Cancel", "Done!" };
		String dialogText = "";
		if (mode == INITIALSYNC) {
			dialogText = "On the newly installed Tablet, Press USB Drive Now. Wait and press Done when the Tablet is finished Synching.";
		} else if (mode == RESYNC) {
			dialogText = "On the Tablet, Press Synchronize with PC now. Wait and press Done when the Tablet is finished Synching.";
		} else {
			dialogText = "Program Error";
		}
		
		int choice = JOptionPane.showOptionDialog(frame, dialogText, "Synchronize with Tablet",
				JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
				options, options[0]);
		return choice;
	}

	public static void copyTabletToLocalFlash(PortableDevice device, PortableDeviceFolderObject dir, File folder) {
    	for(PortableDeviceObject file :  dir.getChildObjects()) {
        	if (file instanceof PortableDeviceFolderObject) {
				System.out.println("directory:" + file.getOriginalFileName());
            	PortableDeviceFolderObject dirI = (PortableDeviceFolderObject)file;

            	// Now create the directory in the local flash folder
        		folder = new File(folder + File.separator + file.getOriginalFileName());
        		if (!folder.exists()) {
        			folder.mkdirs();
        		}
            	
            	// Now iterate through
        		copyTabletToLocalFlash(device, dirI, folder);
				folder = new File(folder + File.separator + ".." + File.separator);
        	} else {
				System.out.println("     file:" + file.getOriginalFileName());
		    	PortableDeviceToHostImpl32 copy = new PortableDeviceToHostImpl32();
            	try {
            		// Copy the files out of the tablet now
            		copy.copyFromPortableDeviceToHost(file.getID(), folder.getAbsolutePath(), device);
            	} catch (COMException ex) {
            	}
        	}
    	}
	}

	public static void cleanTabletData() {
        PortableDeviceManager manager = new PortableDeviceManager();
        
        // Get the first device on the list.
        for (PortableDevice device : manager.getDevices()) {
	        // Connect to the tablet now
	        device.open();
	        
	        // debug output
	        Logger.getInstance().log("cleaning tablet data " + device.getModel());
	
	        // Iterate over deviceObjects
	        for(PortableDeviceObject object : device.getRootObjects()) {
	        	System.out.println(object);
	       	
	            // If the object is a storage object
	            if(object instanceof PortableDeviceStorageObject) {
	                PortableDeviceStorageObject storage = (PortableDeviceStorageObject)object;
	                
	                for(PortableDeviceObject o2 :  storage.getChildObjects()) {
	                	System.out.println(o2.getOriginalFileName());
	                	
	                	// Check if the device object is a folder first
	                	if (o2 instanceof PortableDeviceFolderObject) {
	                		// Check if there is a frc folder
    	                    if (o2.getOriginalFileName().equals("frc")) {
    	                    	
    	                    	// Now list all the content inside this frc folder object
    	                    	PortableDeviceFolderObject frcFolder = (PortableDeviceFolderObject)o2;
    	                		deleteTabletData(device, frcFolder);
    	                    }	                		
	                	}
	            	}              
	            }
	        }
	        device.close();
        }
	}
	
	public static void deleteTabletData(PortableDevice device, PortableDeviceFolderObject dir) {
    	for(PortableDeviceObject file :  dir.getChildObjects()) {
        	if (file instanceof PortableDeviceFolderObject) {
            	PortableDeviceFolderObject dirI = (PortableDeviceFolderObject)file;

            	// Now iterate through
            	deleteTabletData(device, dirI);
				System.out.println("Deleting directory:" + file.getOriginalFileName());
        	} else {
				System.out.println("     Deleting file:" + file.getOriginalFileName());
        	}
        	// Delete either an empty folder or an object
        	file.delete();
    	}
	}
	
	public static void syncWithFlashDrive() throws IOException {
		// IMPORTANT! First, make a backup of all the data we have now, both on the flash drive and on local
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("CST"));
		File localBackupDir = new File(getNonsyncedDirectory() + File.separator + "backups" + File.separator + "local" + File.separator + c.get(Calendar.YEAR) + "-" + c.get(Calendar.MONTH) + "-"
				+ c.get(Calendar.DAY_OF_MONTH) + "-" + c.get(Calendar.HOUR_OF_DAY) + "." + c.get(Calendar.MINUTE) + "." + c.get(Calendar.SECOND));
		localBackupDir.mkdirs();
		FileUtils.copyDirectory(getSyncedDirectory(), new File(localBackupDir + File.separator + "synced"));
		FileUtils.copyDirectory(getUnintegratedDirectory(), new File(localBackupDir + File.separator + "unintegrated"));
		Logger.getInstance().log("backup of local created");
		File flashBackupDir = new File(getNonsyncedDirectory() + File.separator + "backups" + File.separator + "flash" + File.separator + c.get(Calendar.YEAR) + "-" + c.get(Calendar.MONTH) + "-"
				+ c.get(Calendar.DAY_OF_MONTH) + "-" + c.get(Calendar.HOUR_OF_DAY) + "." + c.get(Calendar.MINUTE) + "." + c.get(Calendar.SECOND));
		flashBackupDir.mkdirs();
		FileUtils.copyDirectory(getFlashDriveSyncedDirectory(), new File(flashBackupDir + File.separator + "synced"));
		FileUtils.copyDirectory(getFlashDriveUnintegratedDirectory(), new File(flashBackupDir + File.separator + "unintegrated"));
		Logger.getInstance().log("backup of flash createdd");

		syncLocalAndFlashDirectories();
		Logger.getInstance().log("syncing synced directories done!");

		// Now we copy the contents of the unintegrated files on the flash drive to the local storage
		// We append the data instead of overwriting it
		List<File> fileList = new ArrayList<File>();
		listFilesInDirectory(getFlashDriveUnintegratedDirectory(), fileList);
		for (File file : fileList) {
			File destinationFile = new File(file.getAbsolutePath().replace(getFlashDriveUnintegratedDirectory().getAbsolutePath(), getUnintegratedDirectory().getAbsolutePath()));
			Logger.getInstance().log("destination: " + destinationFile.getAbsolutePath());
			copyFileWithAppend(file, destinationFile);
		}
		Logger.getInstance().log("flash unsynced done");

		// Next, wipe the unintegrated directory on the flash drive. All unintegrated
		// files are now stored locally and ready for integration.
		FileUtils.cleanDirectory(getFlashDriveUnintegratedDirectory());

		Logger.getInstance().log("flash unintegrated wiped");
	}

	private static void copyFileWithAppend(File source, File destination) throws IOException {
		if (!destination.exists()) {
			destination.getParentFile().mkdirs();
			destination.createNewFile();
		}
		if (!source.exists()) {
			throw new IOException("Source file must exist!");
		}
		BufferedReader sourceReader = new BufferedReader(new FileReader(source));
		BufferedWriter destinationWriter = new BufferedWriter(new FileWriter(destination, true));
		String line;
		while ((line = sourceReader.readLine()) != null) {
			destinationWriter.write(line);
			destinationWriter.newLine();
		}
		destinationWriter.flush();
		destinationWriter.close();
		sourceReader.close();
	}

	public static String getRelativePathForLocal(File file) {
		String absolutePath = file.getAbsolutePath();

		// First we find the length of the root path
		int startIndex = GlobalAppHandler.getInstance().getAppData().getLocalLocation().getAbsolutePath().length();
		// Next, we search for the next file separator character after that
		int fileSeparatorIndex = absolutePath.indexOf(File.separator, startIndex + 1);
		// If we remove all of the string before that character, we have the relative path!
		String relativePath = absolutePath.substring(fileSeparatorIndex);
		Logger.getInstance().log("relative path: " + relativePath);
		return relativePath;
	}

	public static String getRelativePathForFlashDrive(File file) {
		String absolutePath = file.getAbsolutePath();
		// First we find the length of the root path
		int startIndex = GlobalAppHandler.getInstance().getAppData().getFlashDriveLocation().getAbsolutePath().length();
		// Next, we search for the next file separator character after that
		int fileSeparatorIndex = absolutePath.indexOf(File.separator, startIndex + 1);
		// If we remove all of the string before that character, we have the relative path!
		String relativePath = absolutePath.substring(fileSeparatorIndex);
		Logger.getInstance().log("relative path: " + relativePath);
		return relativePath;
	}

	public static File getSyncedDirectory() {
		File file = new File(GlobalAppHandler.getInstance().getAppData().getLocalLocation() + File.separator + "synced");
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}

	public static File getUnintegratedDirectory() {
		File file = new File(GlobalAppHandler.getInstance().getAppData().getLocalLocation() + File.separator + "unintegrated");
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}

	public static File getNonsyncedDirectory() {
		File file = new File(GlobalAppHandler.getInstance().getAppData().getLocalLocation() + File.separator + "nonsynced");
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}

	public static File getFlashDriveSyncedDirectory() {
		File file = new File(GlobalAppHandler.getInstance().getAppData().getFlashDriveLocation() + File.separator + "synced");
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}

	public static File getFlashDriveUnintegratedDirectory() {
		File file = new File(GlobalAppHandler.getInstance().getAppData().getFlashDriveLocation() + File.separator + "unintegrated");
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}

	public static void listFilesInDirectory(File directory, List<File> list) {
		Logger.getInstance().log("listFilesInDirectory; directory: " + directory.getAbsolutePath());
		File[] files = directory.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isFile() && !file.isHidden()) {
					list.add(file);
				} else if (file.isDirectory()) {
					listFilesInDirectory(file, list);
				}
			}
		}
	}

	private static void syncLocalAndFlashDirectories() {
		long startTime = System.currentTimeMillis();
		int totalFiles = 0;
		// Get lists of files in both directories
		List<File> localSyncedFiles = new ArrayList<File>();
		listFilesInDirectory(getSyncedDirectory(), localSyncedFiles);
		List<File> flashSyncedFiles = new ArrayList<File>();
		listFilesInDirectory(getFlashDriveSyncedDirectory(), flashSyncedFiles);
		for (File f : localSyncedFiles) {
			Logger.getInstance().log("local: " + f.getAbsolutePath());
		}
		for (File f : flashSyncedFiles) {
			Logger.getInstance().log("flash: " + f.getAbsolutePath());
		}
		// Filter each list to have only relative locations

		List<String> localPaths = new ArrayList<String>();
		List<String> flashPaths = new ArrayList<String>();
		for (File file : localSyncedFiles) {
			localPaths.add(getRelativePathForLocal(file));
		}
		for (File file : flashSyncedFiles) {
			flashPaths.add(getRelativePathForFlashDrive(file));
		}
		Iterator<String> flashIterator = flashPaths.iterator();
		while (flashIterator.hasNext()) {
			String flashPath = flashIterator.next();
			totalFiles++;
			if (localPaths.contains(flashPath)) {
				File flashFile = new File(getFlashDriveSyncedDirectory() + File.separator + flashPath);
				File localFile = new File(getSyncedDirectory() + File.separator + flashPath);
				syncFile(flashFile, localFile);
				flashIterator.remove();
				localPaths.remove(flashPath);
			} else {
				File flashFile = new File(getFlashDriveSyncedDirectory() + File.separator + flashPath);
				File localFile = new File(getSyncedDirectory() + File.separator + flashPath);
				localFile.getParentFile().mkdirs();
				try {
					localFile.createNewFile();
					FileUtils.copyFile(flashFile, localFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
				flashIterator.remove();
			}
		}
		Iterator<String> localIterator = localPaths.iterator();
		while (localIterator.hasNext()) {
			totalFiles++;
			String localPath = localIterator.next();
			if (flashPaths.contains(localPath)) {
				File flashFile = new File(getFlashDriveSyncedDirectory() + File.separator + localPath);
				File localFile = new File(getSyncedDirectory() + File.separator + localPath);
				syncFile(flashFile, localFile);
				flashPaths.remove(localPaths);
				localIterator.remove();
			} else {
				File flashFile = new File(getFlashDriveSyncedDirectory() + File.separator + localPath);
				File localFile = new File(getSyncedDirectory() + File.separator + localPath);
				localFile.getParentFile().mkdirs();
				try {
					localFile.createNewFile();
					FileUtils.copyFile(localFile, flashFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
				localIterator.remove();
			}
		}
		long totalTime = System.currentTimeMillis() - startTime;
		Logger.getInstance().log("Total time for sync: " + totalTime + "ms");
		if (totalFiles != 0) {
			Logger.getInstance().log("Average time per file: " + (totalTime / totalFiles) + "ms");
		}
	}

	private static void syncFile(File file1, File file2) {
		long timestamp1 = file1.lastModified();
		long timestamp2 = file2.lastModified();
		try {
			if (timestamp1 > timestamp2) {
				FileUtils.copyFile(file1, file2);
			} else if (timestamp1 < timestamp2) {
				FileUtils.copyFile(file2, file1);
			} else {
				// If timestamp is the same, we can assume that the files are identical
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void loadLib(String path, String name) {
	    try {
	    	File fileOut = new File(path + name);
	    	System.load(fileOut.getAbsolutePath());
	    } catch (UnsatisfiedLinkError e) {
	    	 Logger.getInstance().log("Native code library failed to load.\n" + e);
	    	 Logger.getInstance().log("Please ensure to jave JRE v7.0 or newer with 32-bit suppport. Not 64-bit.");
	    }
	}
}
