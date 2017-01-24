package de.julielab.bioportal.ontologies.apps;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.bioportal.ontologies.OntologyClassNameExtractor;

public class NameExtractor {

	private static final Logger log = LoggerFactory.getLogger(NameExtractor.class);

	public static final File downloadDir = new File("ontology-download");
	public static final File ontologiesDir = new File(downloadDir.getAbsolutePath() + File.separator + "ontologies");
	public static final File infoDir = new File(downloadDir.getAbsolutePath() + File.separator + "info");

	public static void main(String[] args) throws Exception {
		log.info(
				"Extracting ontology names, synonyms and descriptions from downloaded ontologies and storing them into extracted-class-info.");
		long time = System.currentTimeMillis();
		OntologyClassNameExtractor nameExtractor = new OntologyClassNameExtractor();
		int numOntologies = nameExtractor.run(ontologiesDir, infoDir, new File("extracted-class-info"), getSpecifiedOntologies(args));
		time = System.currentTimeMillis() - time;
		log.info("Extracting names from {} ontologies took {}ms ({}s)",
				new Object[] { numOntologies, time, time / 1000 });

		log.info("Process complete.");
	}

	private static Set<String> getSpecifiedOntologies(String[] args) {
		if (args.length == 0)
			return Collections.emptySet();
		Set<String> acronyms = new HashSet<>();
		for (int i = 0; i < args.length; i++) {
			acronyms.add(args[i]);
		}
		return acronyms;
	}
	
}
