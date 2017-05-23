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

		String mappingsDirPath;
		String apikey;
		// TODO this is currently an interactive-only feature. Should also be
		// possible to specify non-interactively
		String ontosDirPath = null;
		Set<String> ontologiesForDownload = Collections.emptySet();
		if (args.length < 2) {
			System.out.println("Usage: " + MappingDownloader.class.getSimpleName()
					+ " <directory to store ontology class mappings> <api key> [acronym1 acronym2 ...]");
			mappingsDirPath = readLineFromStdInWithMessage("Please specify the download directory:");
			apikey = readLineFromStdInWithMessage("Please specify your BioPortal API key:");
			String[] acronyms = readLineFromStdInWithMessage(
					"Optional: Specify ontology acronyms to restrict the download to, separated by whitespace:").trim()
							.split("\\s");
			if (acronyms.length > 0) {
				ontologiesForDownload = new HashSet<>();
				for (int i = 0; i < acronyms.length; i++) {
					if (acronyms[i].length() > 0)
						ontologiesForDownload.add(acronyms[i]);
				}
			}
			ontosDirPath = readLineFromStdInWithMessage(
					"Optional: Specify the directory where BioPortal ontologies have been downloaded by the BioPortal tools. The mapping download will be restricted to those ontologies.");
		} else {
			mappingsDirPath = args[0];
			apikey = args[1];
			if (args.length > 2) {
				ontologiesForDownload = new HashSet<>();
				for (int i = 2; i < args.length; i++)
					ontologiesForDownload.add(args[i]);
			}
		}
		File mappingsDir = new File(mappingsDirPath);
		mappingsDir.mkdirs();
		File ontosDir = ontosDirPath.trim().length() > 0 ? new File(ontosDirPath) : null;

		try {
			MappingDownloader ontos = new MappingDownloader(apikey);
			ontos.downloadOntologyMappings(mappingsDir, ontosDir, ontologiesForDownload);
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
