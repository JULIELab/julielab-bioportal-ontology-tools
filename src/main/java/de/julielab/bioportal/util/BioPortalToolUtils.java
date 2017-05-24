package de.julielab.bioportal.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
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

	public static boolean isSupportedOntologyFile(File file) {
		String lcfn = file.getName().toLowerCase();
		return lcfn.contains(".obo") || lcfn.contains(".owl") || lcfn.contains(".umls");
	}

	public static boolean isUMLSOntology(File file) {
		String lcfn = file.getName().toLowerCase();
		return lcfn.contains(".umls");
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

	/**
	 * Returns an {@link InputStream} for <tt>file</tt>. Automatically wraps in
	 * an {@link BufferedInputStream} and also in an {@link GZIPInputStream} if
	 * the file name ends with .gz or .gzip.
	 * 
	 * @param file
	 *            The file to read.
	 * @return A buffered input stream.
	 * @throws IOException
	 *             If there is an error during reading.
	 */
	public static InputStream getInputStreamFromFile(File file) throws IOException {
		InputStream is = new FileInputStream(file);
		String lcfn = file.getName().toLowerCase();
		if (lcfn.contains(".gz") || lcfn.contains(".gzip"))
			is = new GZIPInputStream(is);
		return new BufferedInputStream(is);
	}

	/**
	 * Returns an {@link OutputStream} for <tt>file</tt>. Automatically wraps in
	 * an {@link BufferedOutputStream} and also in an {@link GZIPOutputStream}
	 * if the file name ends with .gz or .gzip.
	 * 
	 * @param file
	 *            The file to write.
	 * @return A buffered output stream.
	 * @throws IOException
	 *             If there is an error during stream creation.
	 */
	public static OutputStream getOutputStreamToFile(File file) throws IOException {
		OutputStream os = new FileOutputStream(file);
		String lcfn = file.getName().toLowerCase();
		if (lcfn.contains(".gz") || lcfn.contains(".gzip"))
			os = new GZIPOutputStream(os);
		return new BufferedOutputStream(os);
	}

	public static Reader getReaderFromFile(File file) throws IOException {
		return new BufferedReader(new InputStreamReader(getInputStreamFromFile(file), "UTF-8"));
	}

	public static Writer getWriterToFile(File file) throws IOException {
		return new BufferedWriter(new OutputStreamWriter(getOutputStreamToFile(file), "UTF-8"));
	}

	public static String readLineFromStdIn() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		return br.readLine();
	}

	public static String readLineFromStdInWithMessage(String message) throws IOException {
		System.out.println(message);
		return readLineFromStdIn();
	}
	
	public static boolean readYesNoFromStdInWithMessage(String message) throws IOException {
		String response = "";
		while(!response.equals("y") && !response.equals("yes") && !response.equals("n") && !response.equals("no")) {
			response = readLineFromStdInWithMessage(message + " (y/n)");
			response = response.toLowerCase();
		}
		return response.equals("y") || response.equals("yes");
	}
	
	public static boolean readYesNoFromStdInWithMessage(String message, boolean defaultResponse) throws IOException {
		String response = "";
		String defaultMarker = defaultResponse ? "y" : "n";
		while(!response.equals("y") && !response.equals("yes") && !response.equals("n") && !response.equals("no") && response.trim().length() > 0) {
			response = readLineFromStdInWithMessage(message + " (y/n)["+defaultMarker+"]");
			response = response.toLowerCase();
		}
		return response.equals("y") || response.equals("yes") || response.trim().length() == 0;
	}

}
