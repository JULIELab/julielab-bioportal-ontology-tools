package de.julielab.bioportal.ontologies.apps;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.bioportal.ontologies.OntologyClassNameExtractor;
import de.julielab.bioportal.util.BioPortalToolUtils;

public class NameExtractorApplication {

	private static final Logger log = LoggerFactory.getLogger(NameExtractorApplication.class);

	public static void main(String[] args)
			throws OWLOntologyCreationException, IOException, InterruptedException, ExecutionException {
		File ontologiesDir;
		File ontologyInfosDir;
		File outputDir;
		boolean applyReasoning;
		if (args.length < 4) {
			System.err
					.println("Usage: " + NameExtractorApplication.class.getSimpleName() + " <ontologies dir> <ontologies info dir> <output dir> <apply reasoning: true/false> [<acronym1>,<acronym2>,...]");
			ontologiesDir = new File(BioPortalToolUtils.readLineFromStdInWithMessage("Please specify the ontologies directory:"));
			ontologyInfosDir = new File(BioPortalToolUtils.readLineFromStdInWithMessage("Please specify the ontolgoy info directory:"));
			outputDir = new File(BioPortalToolUtils.readLineFromStdInWithMessage("Please specify the output directory:"));
			applyReasoning = Boolean.parseBoolean(BioPortalToolUtils.readLineFromStdInWithMessage("Please specify whether to apply reasoning (true or false):"));
		} else {
			ontologiesDir = new File(args[0]);
			ontologyInfosDir = new File(args[1]);
			outputDir = new File(args[2]);
			applyReasoning = Boolean.parseBoolean(args[3]);
		}
		log.info(
				"Extracting ontology names, synonyms and descriptions from downloaded ontologies and storing them into {}.",
				outputDir);
		long time = System.currentTimeMillis();
		OntologyClassNameExtractor nameExtractor = new OntologyClassNameExtractor(Executors.newCachedThreadPool(Executors.defaultThreadFactory()), applyReasoning);
		int numOntologies = nameExtractor.run(ontologiesDir, ontologyInfosDir, outputDir, getSpecifiedOntologies(args));
		nameExtractor.shutDown();
		time = System.currentTimeMillis() - time;
		log.info("Extracting names from {} ontologies took {}ms ({}s)",
				new Object[] { numOntologies, time, time / 1000 });

		log.info("Process complete.");
	}

	private static Set<String> getSpecifiedOntologies(String[] args) {
		if (args.length < 4)
			return Collections.emptySet();
		Set<String> acronyms = new HashSet<>();
		for (int i = 3; i < args.length; i++) {
			acronyms.add(args[i]);
		}
		return acronyms;
	}

}
