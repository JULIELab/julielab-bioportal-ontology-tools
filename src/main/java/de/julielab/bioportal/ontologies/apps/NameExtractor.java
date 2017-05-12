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

	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			System.err.println(
					"Usage: <ontologies dir> <ontologies info dir> <output dir> [<acronym1>,<acronym2>,...]");
			System.exit(1);
		}
		File ontologiesDir = new File(args[0]);
		File ontologyInfosDir = new File(args[1]);
		File outputDir = new File(args[2]);
		log.info(
				"Extracting ontology names, synonyms and descriptions from downloaded ontologies and storing them into {}.", outputDir);
		long time = System.currentTimeMillis();
		OntologyClassNameExtractor nameExtractor = new OntologyClassNameExtractor();
		int numOntologies = nameExtractor.run(ontologiesDir, ontologyInfosDir, outputDir, getSpecifiedOntologies(args));
		time = System.currentTimeMillis() - time;
		log.info("Extracting names from {} ontologies took {}ms ({}s)",
				new Object[] { numOntologies, time, time / 1000 });

		log.info("Process complete.");
	}

	private static Set<String> getSpecifiedOntologies(String[] args) {
		if (args.length < 4)
			return Collections.emptySet();
		Set<String> acronyms = new HashSet<>();
		for (int i = 2; i < args.length; i++) {
			acronyms.add(args[i]);
		}
		return acronyms;
	}
	
}
