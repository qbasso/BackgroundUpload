/*
 * @author JPorzuczek
 */
package pl.qbasso.dropboxapp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

// TODO: Auto-generated Javadoc
/**
 * The Class Utils.
 */
public class Utils {

	public static final Pattern WAV = Pattern.compile("(?i)[\\s\\S]+\\.wav");
	public static final String DIR_PATH = "/mnt/sdcard/recorder/";
	public static final int SCAN_INTERVAL = 1000*60*10;
	public static boolean DEBUG_FLAG = true;
	
	/**
	 * Check internet connection.
	 * 
	 * @param ctx
	 *            the ctx
	 * @return true, if successful
	 */
	public static boolean checkInternetConnection(Context ctx) {
		ConnectivityManager manager = (ConnectivityManager) ctx
				.getSystemService(Activity.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		if (info != null && info.isAvailable()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Show toast message.
	 * 
	 * @param ctx
	 *            the ctx
	 * @param text
	 *            the text
	 */
	public static void showToastMessage(Context ctx, String text) {
		Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Explore given dir to maximal depth.
	 * 
	 * @param dir
	 *            the dir
	 * @return the list
	 */
	public static List<String> exploreToMaxDepth(File dir) {
		ArrayList<String> result = new ArrayList<String>();
		try {
			if (dir.isDirectory()) {
				File[] files = dir.listFiles();
				if (files != null) {
					for (int i = 0; i < files.length; ++i) {
						if (files[i].isDirectory()) {
							result.addAll(exploreToMaxDepth(files[i]));
						} else {
							if (WAV.matcher(files[i].getCanonicalPath()).find()) {
								result.add(files[i].getAbsolutePath());
							}
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Find wav files on device. Folders should have *usb* in their names.
	 * Search looks for dirs two levels from given root dir if *usb* pattern is
	 * not found rest of fodler search is abandoned
	 * 
	 * @param dir
	 *            the dir
	 * @param pattern
	 *            the pattern
	 * @param level
	 *            the level
	 * @return the list
	 */
	public static List<String> findWavFilesOnDevice(File dir, String pattern,
			int level) {
		ArrayList<String> result = new ArrayList<String>();
		try {
			if (level < 2) {
				if (dir.isDirectory()) {
					File[] files = dir.listFiles();
					if (files != null) {

						for (int i = 0; i < files.length; ++i) {
							if (files[i].isDirectory()) {
								if (pattern != null
										&& files[i].getAbsolutePath().matches(
												pattern)) {
									result.addAll(exploreToMaxDepth(files[i]));
								} else if (pattern != null) {
									result.addAll(findWavFilesOnDevice(
											files[i], pattern, level + 1));
								}
							} else {
								if (WAV.matcher(files[i].getCanonicalPath())
										.find()) {
									result.add(files[i].getAbsolutePath());
								}
							}
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Compress files. Names of zip entries are the same as file names
	 * 
	 * @param filenames
	 *            the filenames
	 * @param dir
	 *            the dir
	 * @param fileName
	 *            the file name
	 * @return the string
	 */
	public static String compressFiles(List<String> filenames, String dir,
			String fileName) {
		String filePath = dir + fileName;
		byte[] data = new byte[2048];
		String tempName;
		ZipEntry entry;
		int cnt = 0;
		try {
			ZipOutputStream zos;
			BufferedInputStream bos;
			zos = new ZipOutputStream(new FileOutputStream(filePath));
			for (String s : filenames) {
				bos = new BufferedInputStream(new FileInputStream(s));
				int index = s.lastIndexOf('/');
				if (index != -1) {
					tempName = s.substring(index + 1);

				} else {
					tempName = s;
				}
				// tempName =
				// Long.toString(System.currentTimeMillis())+tempName;
				entry = new ZipEntry(tempName);
				zos.putNextEntry(entry);
				while ((cnt = bos.read(data, 0, 2048)) != -1) {
					zos.write(data, 0, cnt);
				}
				zos.closeEntry();
				bos.close();
			}
			zos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return filePath;
	}

	/**
	 * Delete files supplied in array of absolute paths
	 * 
	 * @param mFileList
	 *            the m file list
	 */
	public static void deleteFiles(ArrayList<String> mFileList) {
		if (mFileList != null) {
			for (String s : mFileList) {
				File f = new File(s);
				if (f.exists()) {
					f.delete();
				}
			}
		}
	}

	public static boolean validateEmail(String email) {
		if (email.matches("(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$")) {
			return true;
		}
		return false;
	}

}
