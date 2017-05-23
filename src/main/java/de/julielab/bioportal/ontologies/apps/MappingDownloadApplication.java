package de.julielab.bioportal.ontologies.apps;

import static de.julielab.bioportal.util.BioPortalToolUtils.readLineFromStdInWithMessage;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.bioportal.ontologies.MappingDownloader;

public class MappingDownloadApplication {

	private static final Logger log = LoggerFactory.getLogger(MappingDownloadApplication.class);

	public static void main(String[] args) throws IOException {

		String triplesDirPath;
		String apikey;
		Set<String> ontologiesForDownload = Collections.emptySet();
		if (args.length < 2) {
			System.out.println("Usage: " + MappingDownloader.class.getSimpleName()
					+ " <directory to store ontology class mappings> <api key> [acronym1 acronym2 ...]");
			triplesDirPath = readLineFromStdInWithMessage("Please specify the download directory:");
			apikey = readLineFromStdInWithMessage("Please specify your BioPortal API key:");
			String[] acronyms = readLineFromStdInWithMessage("Optional: Specify ontology acronyms to restrict the download to, separated by whitespace:").split("\\s");
			if (acronyms.length > 0) {
				ontologiesForDownload = new HashSet<>();
				for (int i = 0; i < acronyms.length; i++)
					ontologiesForDownload.add(acronyms[i]);
			}
		} else {
			triplesDirPath = args[0];
			apikey = args[1];
			if (args.length > 2) {
				ontologiesForDownload = new HashSet<>();
				for (int i = 2; i < args.length; i++)
					ontologiesForDownload.add(args[i]);
			}
		}
		File mappingsDir = new File(triplesDirPath);
		mappingsDir.mkdirs();

		try {
			MappingDownloader ontos = new MappingDownloader(apikey);
			ontos.downloadOntologyMappings(mappingsDir, ontologiesForDownload);
			ontos.shutdown();
		} catch (ParseException e) {
			log.error("ParseException: ", e);
		} catch (IOException e) {
			log.error("IOException: ", e);
		} catch (Exception e) {
			log.error("Exception: ", e);
		}
	}

}
