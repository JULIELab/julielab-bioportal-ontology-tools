package de.julielab.bioportal.ontologies.apps;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.bioportal.ontologies.DownloadStats;
import de.julielab.bioportal.ontologies.OntologyClassNameExtractor;
import de.julielab.bioportal.ontologies.OntologyDownloader;

/**
 * Loads down all BioPortal ontologies available and extracts class names. Both
 * is stored into hard-coded default directories relative to the working
 * directory. The only required parameter is the API key.
 * 
 * @author faessler
 *
 */
public class CompleteProcess {

	private static final Logger log = LoggerFactory.getLogger(CompleteProcess.class);

	public static final File downloadDir = new File("ontology-download");
	public static final File ontologiesDir = new File(downloadDir.getAbsolutePath() + File.separator + "ontologies");
	public static final File infoDir = new File(downloadDir.getAbsolutePath() + File.separator + "info");

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println(
					"You must provide your BioPortal API key. You can get one without charge by registering an account on the BioPortal web page.");
			System.exit(1);
		}
		String apiKey = args[0];
		OntologyDownloader downloader = new OntologyDownloader(apiKey);
		log.info("Downloading ontologies...");
		long time = System.currentTimeMillis();
		DownloadStats downloadStats = downloader.downloadOntologies(ontologiesDir, infoDir,
				getSpecifiedOntologies(args));
		time = System.currentTimeMillis() - time;
		log.info("Downloading {} ontologies took {}ms ({}s)",
				new Object[] { downloadStats.getNumOntologiesDownloaded(), time, time / 1000 });
		log.info("Writing download report to downloadreport.txt");
		FileUtils.write(new File("downloadreport.txt"), downloadStats.report(), "UTF-8", false);

		log.info(
				"Extracting ontology names, synonyms and descriptions from downloaded ontologies and storing them into extracted-class-info.");
		time = System.currentTimeMillis();
		OntologyClassNameExtractor nameExtractor = new OntologyClassNameExtractor();
		nameExtractor.run(ontologiesDir, infoDir, new File("extracted-class-info"), getSpecifiedOntologies(args));
		time = System.currentTimeMillis() - time;
		log.info("Extracting names from {} ontologies took {}ms ({}s)",
				new Object[] { downloadStats.getNumOntologiesDownloaded(), time, time / 1000 });
		
		log.info("Process complete.");
	}

	private static Set<String> getSpecifiedOntologies(String[] args) {
		if (args.length == 1)
			return Collections.emptySet();
		Set<String> acronyms = new HashSet<>();
		for (int i = 1; i < args.length; i++) {
			acronyms.add(args[i]);
		}
		return acronyms;
	}

}
