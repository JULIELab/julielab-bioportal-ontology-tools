package de.julielab.bioportal.ontologies;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import de.julielab.bioportal.ontologies.OntologyClassNameExtractor;
import de.julielab.java.utilities.FileUtilities;

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
	
	@Test
	public void filterDeprecatedClassesTrue() throws IOException, OWLOntologyCreationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		boolean applyReasoning = false;
		boolean filterDeprecated = true;
		OntologyClassNameExtractor nameExtractor = new OntologyClassNameExtractor(Executors.newCachedThreadPool(Executors.defaultThreadFactory()), applyReasoning, filterDeprecated);
		String acronym = "QUDTmini";
		File ontologyFile = new File("src/test/resources/QUDTmini.owl.gz");
		
		OWLReasoner reasoner = null;
		
		OntologyLoader ontologyLoader = new OntologyLoader();
		AnnotationPropertySet properties = new AnnotationPropertySet(ontologyLoader.getOntologyManager(),
				new File(""));
		File classesFile = File.createTempFile(acronym, BioPortalToolConstants.SUBMISSION_EXT + ".gz");

		OWLOntology o;
		try {
			o = ontologyLoader.loadOntology(ontologyFile);
		} catch (OWLOntologyCreationException e) {
			throw e;
		}

		//Make private method accessible
		Class<? extends OntologyClassNameExtractor> targetClass = nameExtractor.getClass();
		Method method = targetClass.getDeclaredMethod("writeNames",
			properties.getClass(), classesFile.getClass(), OWLOntology.class, OWLReasoner.class);
		method.setAccessible(true);
		method.invoke(nameExtractor, properties, classesFile, o, reasoner);

		int lines = 0;
		try (BufferedReader br = FileUtilities.getReaderFromFile(classesFile)) {
			while (null != br.readLine()) {
				++lines;
			}
		}
		assertEquals(7, lines);
	}
	
	@Test
	public void filterDeprecatedClassesFalse() throws IOException, OWLOntologyCreationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		boolean applyReasoning = false;
		boolean filterDeprecated = false;
		OntologyClassNameExtractor nameExtractor = new OntologyClassNameExtractor(Executors.newCachedThreadPool(Executors.defaultThreadFactory()), applyReasoning, filterDeprecated);
		String acronym = "QUDTmini";
		File ontologyFile = new File("src/test/resources/QUDTmini.owl.gz");
		
		OWLReasoner reasoner = null;
		
		OntologyLoader ontologyLoader = new OntologyLoader();
		AnnotationPropertySet properties = new AnnotationPropertySet(ontologyLoader.getOntologyManager(),
				new File(""));
		File classesFile = File.createTempFile(acronym, BioPortalToolConstants.SUBMISSION_EXT + ".gz");

		OWLOntology o;
		try {
			o = ontologyLoader.loadOntology(ontologyFile);
		} catch (OWLOntologyCreationException e) {
			throw e;
		}

		//Make private method accessible
		Class<? extends OntologyClassNameExtractor> targetClass = nameExtractor.getClass();
		Method method = targetClass.getDeclaredMethod("writeNames",
			properties.getClass(), classesFile.getClass(), OWLOntology.class, OWLReasoner.class);
		method.setAccessible(true);
		method.invoke(nameExtractor, properties, classesFile, o, reasoner);

		int lines = 0;
		try (BufferedReader br = FileUtilities.getReaderFromFile(classesFile)) {
			while (null != br.readLine()) {
				++lines;
			}
		}
		assertEquals(8, lines);
	}
	
	/**
	 * The test ontology OBIBmini contains an <owl:deprecated> tag whose Literal resolves to Optional.empty.
	 * If it isn't checked, this will crash the program, if used anyway.
	 * @throws IOException
	 * @throws OWLOntologyCreationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	@Test
	public void checkOptionalLiteralEmpty() throws IOException, OWLOntologyCreationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		boolean applyReasoning = false;
		boolean filterDeprecated = false;
		OntologyClassNameExtractor nameExtractor = new OntologyClassNameExtractor(Executors.newCachedThreadPool(Executors.defaultThreadFactory()), applyReasoning, filterDeprecated);
		String acronym = "OBIBmini";
		File ontologyFile = new File("src/test/resources/OBIBmini.owl.gz");
		
		OWLReasoner reasoner = null;
		
		OntologyLoader ontologyLoader = new OntologyLoader();
		AnnotationPropertySet properties = new AnnotationPropertySet(ontologyLoader.getOntologyManager(),
				new File(""));
		File classesFile = File.createTempFile(acronym, BioPortalToolConstants.SUBMISSION_EXT + ".gz");

		OWLOntology o;
		try {
			o = ontologyLoader.loadOntology(ontologyFile);
		} catch (OWLOntologyCreationException e) {
			throw e;
		}

		//Make private method accessible
		Class<? extends OntologyClassNameExtractor> targetClass = nameExtractor.getClass();
		Method method = targetClass.getDeclaredMethod("writeNames",
			properties.getClass(), classesFile.getClass(), OWLOntology.class, OWLReasoner.class);
		method.setAccessible(true);
		method.invoke(nameExtractor, properties, classesFile, o, reasoner);
	}
}
