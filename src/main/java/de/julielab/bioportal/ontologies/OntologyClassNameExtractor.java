package de.julielab.bioportal.ontologies;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.io.UnparsableOntologyException;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.julielab.bioportal.ontologies.data.OntologyClass;
import de.julielab.bioportal.ontologies.data.OntologyClassParents;
import de.julielab.bioportal.ontologies.data.OntologyClassSynonyms;
import de.julielab.bioportal.util.BioPortalToolUtils;
import uk.ac.manchester.cs.jfact.JFactFactory;

/**
 * The error "[Fatal Error] :1:1: Content is not allowed in prolog." for OBO
 * ontologies is just a STDERR leak before the next parser is tried by the OWL
 * API. Just ignore it. https://github.com/owlcs/owlapi/issues/550
 * 
 * @author faessler
 *
 */
public class OntologyClassNameExtractor {

	private static final Logger log = LoggerFactory.getLogger(OntologyClassNameExtractor.class);
	private static final Logger logUnparsableOntologies = LoggerFactory
			.getLogger(OntologyClassNameExtractor.class.getCanonicalName() + ".unparsable");

	private Gson gson;

	private ExecutorService executor;
	private OWLReasonerFactory reasonerFactory;

	public OntologyClassNameExtractor() {
		this.gson = BioPortalToolUtils.getGson();
		this.executor = Executors.newFixedThreadPool(8);
		reasonerFactory = new JFactFactory();
	}

	public int run(File input, File submissionsDirectory, File output)
			throws OWLOntologyCreationException, IOException, InterruptedException, ExecutionException {
		return run(input, submissionsDirectory, output, null);
	}

	public int run(File input, File submissionsDirectory, File outputDir, Set<String> ontologiesToExtract)
			throws IOException, OWLOntologyCreationException, InterruptedException, ExecutionException {
		if (!outputDir.exists())
			outputDir.mkdirs();

		int numOntologies = 0;

		if (!input.isDirectory()) {
			log.error("{} is not a directory. Please specify the directory containing the ontology files.", input);
			return 0;
		}

		if (outputDir.isFile())
			throw new IllegalArgumentException(
					"The output path \"" + outputDir.getAbsolutePath() + "\" is a file but should be a directory.");
		else if (!outputDir.exists()) {
			log.debug("Creating output directory \"{}\"", outputDir);
			outputDir.mkdirs();
		}

		if (ontologiesToExtract != null && !ontologiesToExtract.isEmpty())
			log.info("Extracting class names for ontologies with acronyms {} in directory {}", ontologiesToExtract,
					input);
		else
			log.info("Extracting class names for all ontologies in {}", input);

		File[] files = input.listFiles();
		List<Future<Void>> futures = new ArrayList<>(files.length);
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (ontologiesToExtract != null && !ontologiesToExtract.isEmpty()
					&& !ontologiesToExtract.contains(BioPortalToolUtils.getAcronymFromFileName(file)))
				continue;
			Future<Void> future = executor.submit(new NameExtractorWorker(file, submissionsDirectory, outputDir));
			futures.add(future);
			++numOntologies;
		}
		for (Future<Void> future : futures)
			future.get();
		log.info("Shutting down executor service.");
		executor.shutdown();
		return numOntologies;
	}

	private class NameExtractorWorker implements Callable<Void> {

		private File file;
		private File submissionsDirectory;
		private File outputDir;
		private OntologyLoader ontologyLoader;

		public NameExtractorWorker(File file, File submissionsDirectory, File outputDir) {
			this.file = file;
			this.submissionsDirectory = submissionsDirectory;
			this.outputDir = outputDir;
			this.ontologyLoader = new OntologyLoader();
		}

		@Override
		public Void call() throws Exception {
			String lcfn = file.getName().toLowerCase();
			if (lcfn.contains(".obo") || lcfn.contains(".owl") || lcfn.contains(".umls") || file.isDirectory()) {
				try {
					extractNamesForOntology(file, submissionsDirectory, outputDir, ontologyLoader);
				} catch (UnparsableOntologyException e) {
					logUnparsableOntologies.error("{}", file);
				}
			} else {
				log.debug("Ignoring file \"{}\" because it doesn't look like an ontology file", file);
			}
			return null;
		}

	}

	private void extractNamesForOntology(File ontologyFileOrDirectory, File submissionsDirectory, File outputDir,
			OntologyLoader ontologyLoader) throws IOException, OWLOntologyCreationException {
		log.debug("Processing file or directory \"{}\"", ontologyFileOrDirectory);
		String acronym = BioPortalToolUtils.getAcronymFromFileName(ontologyFileOrDirectory);
		File submissionFile = new File(submissionsDirectory.getAbsolutePath() + File.separator + acronym
				+ BioPortalToolConstants.SUBMISSION_EXT);
		AnnotationPropertySet properties = new AnnotationPropertySet(ontologyLoader.getOntologyManager(),
				submissionFile);
		File classesFile = new File(
				outputDir.getAbsolutePath() + File.separator + acronym + BioPortalToolConstants.CLASSES_EXT);
		if (classesFile.exists() && classesFile.length() > 0) {
			log.info("Classes file {} already exists and is not empty. Not extracting class names again.", classesFile);
			return;
		}

		File ontologyFile = ontologyFileOrDirectory;
		// Sometimes there are multiple files associated with one ontology where
		// a main file imports the others.
		if (ontologyFile.isDirectory()) {
			File[] mainFileCandidates = ontologyFileOrDirectory.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return BioPortalToolUtils.getAcronymFromFileName(name.toLowerCase())
							.equals(BioPortalToolUtils.getAcronymFromFileName(acronym.toLowerCase()));
				}
			});
			if (mainFileCandidates == null || mainFileCandidates.length == 0) {
				log.error(
						"For ontology {} a directory of files is given. Could not identify the main file. Skipping this ontology.",
						acronym);
				return;
			} else if (mainFileCandidates.length > 1) {
				log.error(
						"For ontology {} a directory of files is given. Main file name is ambiguous: {}. Skipping this ontology",
						acronym, mainFileCandidates);
				return;
			}
			ontologyFile = mainFileCandidates[0];
			log.debug("Identified file {} as the main file for ontology {}", ontologyFile, acronym);
		}

		String lcfn = ontologyFile.getName().toLowerCase();
		InputStream is = new FileInputStream(ontologyFile);
		if (lcfn.endsWith(".gzip") || lcfn.endsWith(".gz"))
			is = new GZIPInputStream(is);
		OWLOntology o;
		try {
			o = ontologyLoader.loadOntology(is);
		} catch (Error e) {
			log.error("Error while loading ontology {}.", acronym);
			throw e;
		}

		OWLReasoner reasoner = reasonerFactory.createReasoner(o);
		
		List<OntologyClass> classNames = new ArrayList<>();
		Stream<OWLClass> classesInSignature = o.classesInSignature(Imports.INCLUDED);
		for (Iterator<OWLClass> iterator = classesInSignature.iterator(); iterator.hasNext();) {
			OWLClass c = iterator.next();

			if (determineObsolete(ontologyFile, o, c, properties))
				continue;

			if (c.getIRI().toString().endsWith("C16843")) {
				Stream<OWLClassExpression> subClasses = EntitySearcher.getSubClasses(c, o);
				subClasses.map(OWLClassExpression::asOWLClass).map(OWLClass::getIRI)
						.forEach(id -> System.out.println(id));
			}

			String preferredName = determinePreferredName(o, c, properties);
			OntologyClassSynonyms synonyms = determineSynonyms(o, c, properties);
			String definition = determineDefinition(o, c, properties);
			OntologyClassParents ontologyClassParents = determineClassParents(o, c, reasoner);

			OntologyClass ontologyClass = new OntologyClass();
			ontologyClass.id = c.getIRI().toString();
			ontologyClass.prefLabel = preferredName;
			if (synonyms.synonyms != null && !synonyms.synonyms.isEmpty())
				ontologyClass.synonym = synonyms;
			if (!StringUtils.isBlank(definition))
				ontologyClass.definition = Arrays.asList(definition);
			if (ontologyClassParents.parents != null && !ontologyClassParents.parents.isEmpty())
				ontologyClass.parents = ontologyClassParents;

			classNames.add(ontologyClass);
		}

		try (OutputStream os = new FileOutputStream(classesFile)) {
			log.debug("Writing extracted class names for ontology {} to {}", acronym, classesFile);
			IOUtils.write(gson.toJson(classNames) + "\n", os, "UTF-8");
		}

		ontologyLoader.clearLoadedOntologies();
	}

	/**
	 * Returns the superclasses of <tt>c</tt> as IRI strings wrapped in an
	 * {@link OntologyClassParents} instance. Omits anonymous classes.
	 * 
	 * @param o
	 * @param c
	 * @param reasoner
	 * @return
	 */
	private OntologyClassParents determineClassParents(OWLOntology o, OWLClass c, OWLReasoner reasoner) {
		// Stream<OWLClassExpression> superClasses =
		// EntitySearcher.getSuperClasses(c, o);
		Stream<OWLClass> superClasses = reasoner.getSuperClasses(c).entities();
		OntologyClassParents classParents = new OntologyClassParents();
		for (Iterator<OWLClass> iterator = superClasses.iterator(); iterator.hasNext();) {
			OWLClassExpression classExpr = iterator.next();
			if (!classExpr.isAnonymous()) {
				OWLClass owlClass = classExpr.asOWLClass();
				classParents.addParent(owlClass.getIRI().toString());
			}
		}

		return classParents;
	}

	/**
	 * Returns the first non-null definition on a definition annotation property
	 * or null if none could be found.
	 * 
	 * @param o
	 * @param c
	 * @param properties
	 * @return
	 */
	private String determineDefinition(OWLOntology o, OWLClass c, AnnotationPropertySet properties) {
		for (OWLAnnotationProperty definitionProp : properties.getDefinitionProps()) {
			Stream<OWLAnnotation> definitionAnnotations = EntitySearcher.getAnnotations(c, o, definitionProp);
			for (Iterator<OWLAnnotation> iterator = definitionAnnotations.iterator(); iterator.hasNext();) {
				String definition;
				OWLAnnotation owlAnnotation = iterator.next();
				OWLAnnotationValue value = owlAnnotation.getValue();
				if (value instanceof OWLLiteral)
					definition = ((OWLLiteral) value).getLiteral();
				else
					definition = value.toString();
				if (!StringUtils.isBlank(definition))
					return definition;
			}
		}
		return null;
	}

	/**
	 * In contrast the original BioPortal approach where only synonyms from the
	 * first existing annotation property are used, we currently use the
	 * synonyms of all eligible properties we can find. Might change in the
	 * future if there is too much garbage.
	 * 
	 * @param o
	 * @param c
	 * @param properties
	 * @return
	 */
	private OntologyClassSynonyms determineSynonyms(OWLOntology o, OWLClass c, AnnotationPropertySet properties) {
		List<String> synonyms = new ArrayList<>();
		for (OWLAnnotationProperty synonymProp : properties.getSynonymProps()) {
			Stream<OWLAnnotation> synonymAnnotations = EntitySearcher.getAnnotations(c, o, synonymProp);
			for (Iterator<OWLAnnotation> iterator = synonymAnnotations.iterator(); iterator.hasNext();) {
				String synonym;
				OWLAnnotation owlAnnotation = iterator.next();
				OWLAnnotationValue value = owlAnnotation.getValue();
				if (value instanceof OWLLiteral) {
					synonym = ((OWLLiteral) value).getLiteral();
				} else
					synonym = value.toString();
				{

					if (!StringUtils.isBlank(synonym))
						synonyms.add(synonym);
				}
			}
		}
		OntologyClassSynonyms classSynonyms = new OntologyClassSynonyms();
		classSynonyms.synonyms = synonyms;
		return classSynonyms;
	}

	/**
	 * <p>
	 * Returns the first property value that is not null or blank. If no
	 * property is set or has a non-null value, the fragmnet of the class IRI is
	 * returned.
	 * </p>
	 * <p>
	 * This approach corresponds to the BioPortal approach got on the mailing
	 * list from Michael Dorf: <blockquote> We first determine whether the
	 * prefLabelProperty, synonymProperty, definitionProperty are set and use
	 * them. If those aren't set, we default to skos:prefLabel and rdfs:label in
	 * that order. If no skos:prefLabel or rdfs:label exists, we use the last
	 * fragment of the URI of the class as the prefLabel. A similar rule applies
	 * to synonyms and definitions using the properties skos:altLabel and
	 * skos:definition respectively. </blockquote>
	 * 
	 * @param o
	 * @param c
	 * @param properties
	 * @return
	 */
	private String determinePreferredName(OWLOntology o, OWLClass c, AnnotationPropertySet properties) {
		for (OWLAnnotationProperty prefNameProp : properties.getPrefNameProps()) {
			Stream<OWLAnnotation> prefNameAnnotations = EntitySearcher.getAnnotations(c, o, prefNameProp);
			for (Iterator<OWLAnnotation> iterator = prefNameAnnotations.iterator(); iterator.hasNext();) {
				String preferredName;
				OWLAnnotation owlAnnotation = iterator.next();
				OWLAnnotationValue value = owlAnnotation.getValue();
				if (value instanceof OWLLiteral)
					preferredName = ((OWLLiteral) value).getLiteral();
				else
					preferredName = value.toString();
				return preferredName;
			}
		}
		// If we came here, we did not find any preferred name. Use the URI
		// fragment.
		return c.getIRI().getRemainder().orElse(null);
	}

	private boolean determineObsolete(File ontologyFile, OWLOntology o, OWLClass c, AnnotationPropertySet properties) {
		boolean isObsolete = false;
		for (OWLAnnotationProperty obsoleteProp : properties.getObsoleteProps()) {
			Stream<OWLAnnotation> obsoleteAnnotations = EntitySearcher.getAnnotations(c, o, obsoleteProp);
			for (Iterator<OWLAnnotation> iterator = obsoleteAnnotations.iterator(); iterator.hasNext();) {
				OWLAnnotation owlAnnotation = iterator.next();
				String literal = ((OWLLiteral) owlAnnotation.getValue()).getLiteral().toLowerCase();
				if (!literal.equals("true") && !literal.equals("false"))
					log.warn("The obsolete property value of class {} of ontology {} is neither true nor false",
							c.getIRI(), ontologyFile);
				// for the weird case that there are multiple obsolete
				// annotations we consider a class obsolete if at least one
				// property says so
				isObsolete |= Boolean.parseBoolean(literal);
			}
		}
		return isObsolete;
	}

}
