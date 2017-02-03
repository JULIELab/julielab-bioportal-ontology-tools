package de.julielab.bioportal.ontologies.apps;

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

public class OntologyDownloadApplication {

	private static final Logger log = LoggerFactory.getLogger(OntologyDownloadApplication.class);
	
	public static void main(String[] args) throws ParseException, IOException, BioPortalOntologyToolsException, InterruptedException, ExecutionException {
		if (args.length < 3) {
			System.err.println(
					"Usage: <ontologies dir> <ontologies info dir> <BioPortal API Key> [<acronym1>,<acronym2>,...]");
			System.exit(1);
		}
		File ontologiesDir = new File(args[0]);
		File ontologyInfosDir = new File(args[1]);
		String apiKey = args[2];
		OntologyDownloader downloader = new OntologyDownloader(apiKey);
		log.info("Downloading ontologies...");
		long time = System.currentTimeMillis();
		DownloadStats downloadStats = downloader.downloadOntologies(ontologiesDir, ontologyInfosDir,
				getSpecifiedOntologies(args));
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
