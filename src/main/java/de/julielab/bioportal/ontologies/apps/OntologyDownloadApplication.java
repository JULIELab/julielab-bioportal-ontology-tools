package de.julielab.bioportal.ontologies.apps;

import static de.julielab.bioportal.util.BioPortalToolUtils.readLineFromStdInWithMessage;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.apache.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.bioportal.ontologies.DownloadStats;
import de.julielab.bioportal.ontologies.OntologyDownloader;
import de.julielab.bioportal.util.BioPortalOntologyToolsException;
import de.julielab.bioportal.util.BioPortalToolUtils;

public class OntologyDownloadApplication {

	private static final Logger log = LoggerFactory.getLogger(OntologyDownloadApplication.class);

	public static void main(String[] args) throws ParseException, IOException, BioPortalOntologyToolsException,
			InterruptedException, ExecutionException {
		File ontologiesDir;
		File ontologyInfosDir;
		String apiKey;
		Set<String> ontologiesForDownload = new HashSet<>();
		if (args.length < 3) {
			System.err.println("Usage: " + OntologyDownloadApplication.class.getSimpleName()
					+ "<ontologies dir> <ontologies info dir> <BioPortal API Key> [<acronym1>,<acronym2>,...]");
			ontologiesDir = new File(BioPortalToolUtils.readLineFromStdInWithMessage("Please specify the directory to download ontologies to:"));
			ontologyInfosDir = new File(BioPortalToolUtils.readLineFromStdInWithMessage("Please specify the directory to store ontology meta information to:"));
			apiKey = BioPortalToolUtils.readLineFromStdInWithMessage("Please specify your BioPortal API key:");
			String[] acronyms = readLineFromStdInWithMessage("Optional: Specify ontology acronyms to restrict the download to, separated by whitespace:").split("\\s");
			if (acronyms.length > 0) {
				ontologiesForDownload = new HashSet<>();
				for (int i = 0; i < acronyms.length; i++)
					ontologiesForDownload.add(acronyms[i]);
			}
		} else {
			ontologiesDir = new File(args[0]);
			ontologyInfosDir = new File(args[1]);
			apiKey = args[2];
			ontologiesForDownload = getSpecifiedOntologies(args);
		}
		OntologyDownloader downloader = new OntologyDownloader(apiKey);
		log.info("Downloading ontologies...");
		long time = System.currentTimeMillis();
		DownloadStats downloadStats = downloader.downloadOntologies(ontologiesDir, ontologyInfosDir,
				ontologiesForDownload);
		time = System.currentTimeMillis() - time;
		log.info("Downloading {} ontologies took {}ms ({}s)",
				new Object[] { downloadStats.getNumOntologiesDownloaded(), time, time / 1000 });
		log.info("Writing download report to downloadreport.txt");
		FileUtils.write(new File("downloadreport.txt"), downloadStats.report(), "UTF-8", false);
	}

	private static Set<String> getSpecifiedOntologies(String[] args) {
		if (args.length == 3)
			return Collections.emptySet();
		Set<String> acronyms = new HashSet<>();
		for (int i = 3; i < args.length; i++) {
			acronyms.add(args[i]);
		}
		return acronyms;
	}
}
