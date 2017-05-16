package de.julielab.bioportal.ontologies;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import de.julielab.bioportal.ontologies.OntologyClassNameExtractor;

public class OntologyClassNameExtractorTest {
	@Test
	public void testRun() throws OWLOntologyCreationException, IOException, InterruptedException, ExecutionException {
		OntologyClassNameExtractor nameExtractor = new OntologyClassNameExtractor();
		nameExtractor.run(new File("src/test/resources/download-test/ontologies"), new File("src/test/resources/download-test/info"), new File("src/test/resources/ontologies-class-names-output"));
	}
	
	@Test
	public void testMuh() throws OWLOntologyCreationException, IOException, InterruptedException, ExecutionException {
		OntologyClassNameExtractor nameExtractor = new OntologyClassNameExtractor();
		nameExtractor.run(new File("ontostmp"), new File("ontosinfotmp"), new File("out"));
	}
}
