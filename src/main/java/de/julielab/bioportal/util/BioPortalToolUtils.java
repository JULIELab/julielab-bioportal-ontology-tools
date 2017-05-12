package de.julielab.bioportal.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

	public static AtomicInteger fixUmlsFile(File original, File target) throws BioPortalOntologyToolsException {
		AtomicInteger removedLines = new AtomicInteger(0);
		InputStream is = null;
		try {
			is = Files.newInputStream(original.toPath());
			OutputStream os = Files.newOutputStream(target.toPath());
			boolean gzip = original.getName().toLowerCase().contains(".gz")
					|| original.getName().toLowerCase().contains(".gzip");
			if (gzip) {
				is = new GZIPInputStream(is);
				os = new GZIPOutputStream(os);
			}

			// match lines that contain """ and also ''' because then ''' is
			// most likely not meant as a string quotation delimiter but as a
			// string value that causes the parser to mistakenly end the string
			// which in turn produces a parser error
			Matcher m = Pattern.compile("\"\"\".*'''").matcher("");
			try (Stream<String> lines = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8"))).lines();
					BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, Charset.forName("UTF-8")))) {
				Stream<String> filteredLines = lines.filter(l -> {
					m.reset(l);
					if (m.find()) {
						removedLines.incrementAndGet();
						return false;
					}
					return true;
				});
				filteredLines.forEach(l -> {
					try {
						bw.write(l + "\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}
		} catch (IOException e) {
			throw new BioPortalOntologyToolsException(e);
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
				throw new BioPortalOntologyToolsException(e);
			}
		}

		return removedLines;
	}

}
