package de.julielab.bioportal.ontologies;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

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
		// TODO allow to pass the service
		this.executor = Executors.newFixedThreadPool(8);
		reasonerFactory = new org.semanticweb.HermiT.ReasonerFactory();
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
			Future<Void> future = executor
					.submit(new NameExtractorWorker(file, submissionsDirectory, input, outputDir));
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
		private File ontosDir;

		public NameExtractorWorker(File file, File submissionsDirectory, File ontosDir, File outputDir) {
			this.file = file;
			this.submissionsDirectory = submissionsDirectory;
			this.ontosDir = ontosDir;
			this.outputDir = outputDir;
			this.ontologyLoader = new OntologyLoader();
		}

		@Override
		public Void call() throws Exception {
			if (BioPortalToolUtils.isSupportedOntologyFile(file) || file.isDirectory()) {
				try {
					extractNamesForOntology(file, submissionsDirectory, outputDir, ontologyLoader);
				} catch (UnparsableOntologyException e) {
					log.error("Could not parse ontology file {}", file);
					if (BioPortalToolUtils.isUMLSOntology(file)) {
						log.warn("The unparsable ontology is in UMLS format. Those have sometimes issues by chemical"
								+ " names containing the character sequence ''' which is mistakenly interpreted as a string"
								+ " end quote for \"\"\" by the turtle parser in OWL API 5.x. It will be tried to remove such"
								+ " lines and then try parsing again.");
						File backupDir = new File(ontosDir.getAbsoluteFile().getParentFile().getAbsolutePath()
								+ File.separator + ontosDir.getName() + "-backup");
						if (!backupDir.exists())
							backupDir.mkdir();
						File backupFile = new File(backupDir.getAbsolutePath() + File.separator + file.getName());
						log.info("Creating backup of file {} to {}", file, backupFile);
						if (!backupFile.exists())
							Files.copy(file.toPath(), backupFile.toPath());
						else
							log.info("File {} already exists, skipping.", backupFile);
						log.warn(
								"Replacing file {} with a copy where lines in question have been removed. Please note that the origin file is overwritten.",
								file);
						Files.delete(file.toPath());
						AtomicInteger removedLines = BioPortalToolUtils.fixUmlsFile(backupFile, file);
						log.info("{} lines have been removed from {}", removedLines, backupFile);
						try {
							extractNamesForOntology(file, submissionsDirectory, outputDir, ontologyLoader);
						} catch (UnparsableOntologyException e2) {
							log.error(
									"Fixed file also couldn't be parsed. Deleting fixed file and giving up. The backup file is left at {}",
									backupFile);
							logUnparsableOntologies.error("File: {}", file, e);
							Files.delete(file.toPath());
						}
					} else {
						logUnparsableOntologies.error("File: {}", file, e);
					}
				}
			} else {
				log.debug("Ignoring file \"{}\" because it doesn't look like an ontology file", file);
			}
			return null;
		}

	}

	private void extractNamesForOntology(File ontologyFileOrDirectory, File submissionsDirectory, File outputDir,
			OntologyLoader ontologyLoader) throws IOException, OWLOntologyCreationException {
		log.info("Processing file or directory \"{}\"", ontologyFileOrDirectory);
		String acronym = BioPortalToolUtils.getAcronymFromFileName(ontologyFileOrDirectory);
		File submissionFile = new File(submissionsDirectory.getAbsolutePath() + File.separator + acronym
				+ BioPortalToolConstants.SUBMISSION_EXT + ".gz");
		AnnotationPropertySet properties = new AnnotationPropertySet(ontologyLoader.getOntologyManager(),
				submissionFile);
		File classesFile = new File(
				outputDir.getAbsolutePath() + File.separator + acronym + BioPortalToolConstants.CLASSES_EXT + ".gz");
		if (classesFile.exists() && classesFile.length() > 0) {
			log.info("Classes file {} already exists and is not empty. Not extracting class names again.", classesFile);
			return;
		}

		OWLOntology o;
		try {
			log.debug("Loading ontology from {} {}", ontologyFileOrDirectory.isFile() ? "file" : "directory", ontologyFileOrDirectory);
			o = ontologyLoader.loadOntology(ontologyFileOrDirectory);
			log.trace("Loading done for {}", ontologyFileOrDirectory);
		} catch (Error e) {
			log.error("Error while loading ontology {}.", acronym);
			throw e;
		}

		OWLReasoner reasoner = reasonerFactory != null ? reasonerFactory.createReasoner(o) : null;

		log.debug("Writing extracted class names for ontology {} to {}", acronym, classesFile);
		try (OutputStream os = BioPortalToolUtils.getOutputStreamToFile(classesFile)) {
			Stream<OWLClass> classesInSignature = o.classesInSignature(Imports.INCLUDED);
			for (Iterator<OWLClass> iterator = classesInSignature.iterator(); iterator.hasNext();) {
				OWLClass c = iterator.next();

				if (determineObsolete(o, c, properties)) {
					log.trace("Excluding obsolete class {}", c.getIRI());
					continue;
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

				IOUtils.write(gson.toJson(ontologyClass) + "\n", os, "UTF-8");
			}

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
		Stream<OWLClassExpression> superClasses = reasoner != null
				? reasoner.getSuperClasses(c).entities().map(OWLClassExpression.class::cast)
				: EntitySearcher.getSuperClasses(c, o);
		OntologyClassParents classParents = new OntologyClassParents();
		for (Iterator<OWLClassExpression> iterator = superClasses.iterator(); iterator.hasNext();) {
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

	private boolean determineObsolete(OWLOntology o, OWLClass c, AnnotationPropertySet properties) {
		boolean isObsolete = false;
		for (OWLAnnotationProperty obsoleteProp : properties.getObsoleteProps()) {
			Stream<OWLAnnotation> obsoleteAnnotations = EntitySearcher.getAnnotations(c, o, obsoleteProp);
			for (Iterator<OWLAnnotation> iterator = obsoleteAnnotations.iterator(); iterator.hasNext();) {
				OWLAnnotation owlAnnotation = iterator.next();
				String literal = ((OWLLiteral) owlAnnotation.getValue()).getLiteral().toLowerCase();
				if (!literal.equals("true") && !literal.equals("false"))
					log.warn("The obsolete property value of class {} of ontology {} is neither true nor false",
							c.getIRI(), o.getOntologyID());
				// for the weird case that there are multiple obsolete
				// annotations we consider a class obsolete if at least one
				// property says so
				isObsolete |= Boolean.parseBoolean(literal);
			}
		}
		return isObsolete;
	}

}
