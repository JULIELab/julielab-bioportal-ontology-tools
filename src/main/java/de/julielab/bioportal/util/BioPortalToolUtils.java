package de.julielab.bioportal.util;

import java.io.File;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BioPortalToolUtils {
	public static String getAcronymFromFileName(String filename) {
		if (filename.indexOf('.') != -1)
			return filename.substring(0, filename.indexOf('.'));
		return filename;
	}

	public static String getAcronymFromFileName(File ontologyFile) {
		return getAcronymFromFileName(ontologyFile.getName());
	}

	/**
	 * Returns a Gson instance configured for BioPortal data.
	 * 
	 * @return
	 */
	public static Gson getGson() {
		return new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssX").create();
	}
}
