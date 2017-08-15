package de.julielab.bioportal.ontologies;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.julielab.bioportal.ontologies.data.Submission;
import de.julielab.bioportal.util.BioPortalToolUtils;
import de.julielab.java.utilities.FileUtilities;

/**
 * This class defines the relevant annotation properties as given on the
 * BioPortal mailing list from Michael Dorf: <blockquote> We first determine
 * whether the prefLabelProperty, synonymProperty, definitionProperty are set
 * and use them. If those aren't set, we default to skos:prefLabel and
 * rdfs:label in that order. If no skos:prefLabel or rdfs:label exists, we use
 * the last fragment of the URI of the class as the prefLabel. A similar rule
 * applies to synonyms and definitions using the properties skos:altLabel and
 * skos:definition respectively. </blockquote>
 * 
 * @author faessler
 *
 */
public class AnnotationPropertySet {
	private static final Logger log = LoggerFactory.getLogger(AnnotationPropertySet.class);

	private List<OWLAnnotationProperty> prefNameProps = new ArrayList<>();
	private List<OWLAnnotationProperty> synonymProps = new ArrayList<>();
	private List<OWLAnnotationProperty> definitionProps = new ArrayList<>();
	private List<OWLAnnotationProperty> obsoleteProps = new ArrayList<>();

	public AnnotationPropertySet(OWLOntologyManager ontologyManager, Submission submission) {
		setupAnnotationProperties(ontologyManager, submission);
	}

	public AnnotationPropertySet(OWLOntologyManager ontologyManager, File submissionFile)
			throws FileNotFoundException, IOException {
		Gson gson = BioPortalToolUtils.getGson();
		Submission submission = null;
		if (submissionFile.exists()) {
			String submissionString = IOUtils.toString(FileUtilities.getInputStreamFromFile(submissionFile),
					Charset.forName("UTF-8"));
			submission = gson.fromJson(submissionString, Submission.class);
		} else {
			log.debug("No submission file {} was found, using default annotation properties.", submissionFile);
		}
		setupAnnotationProperties(ontologyManager, submission);
	}

	private void setupAnnotationProperties(OWLOntologyManager ontologyManager, Submission submission) {
		OWLDataFactory df = ontologyManager.getOWLDataFactory();
		// add the properties given in the submission first so they are checked
		// at first
		log.trace("Submission {} specifies the following properties:", (submission != null ? submission.id : null));
		if (submission == null)
			log.trace("None.");
		if (submission != null) {
			if (submission.prefLabelProperty != null) {
				prefNameProps.add(df.getOWLAnnotationProperty(IRI.create(submission.prefLabelProperty)));
				log.trace("Preferred label: {}", submission.prefLabelProperty);
			}
			if (submission.synonymProperty != null) {
				synonymProps.add(df.getOWLAnnotationProperty(IRI.create(submission.synonymProperty)));
				log.trace("Synonyms: {}", submission.synonymProperty);
			}
			if (submission.definitionProperty != null) {
				definitionProps.add(df.getOWLAnnotationProperty(IRI.create(submission.definitionProperty)));
				log.trace("Definition: {}", submission.definitionProperty);
			}
			if (submission.obsoleteProperty != null) {
				obsoleteProps.add(df.getOWLAnnotationProperty(IRI.create(submission.obsoleteProperty)));
				log.trace("Obsolete classes: {}", submission.obsoleteProperty);
			}
		}
		addDefaultAnnotationProperties(ontologyManager);
	}

	public List<OWLAnnotationProperty> getPrefNameProps() {
		return prefNameProps;
	}

	public List<OWLAnnotationProperty> getSynonymProps() {
		return synonymProps;
	}

	public List<OWLAnnotationProperty> getDefinitionProps() {
		return definitionProps;
	}

	public List<OWLAnnotationProperty> getObsoleteProps() {
		return obsoleteProps;
	}

	/**
	 * Assemble all default annotation properties for the information we seek.
	 * 
	 * @param ontologyManager
	 */
	private void addDefaultAnnotationProperties(OWLOntologyManager ontologyManager) {
		OWLDataFactory df = ontologyManager.getOWLDataFactory();

		prefNameProps.add(df.getRDFSLabel());
		prefNameProps.add(df.getOWLAnnotationProperty(IRI.create("http://www.w3.org/2004/02/skos/core#prefLabel")));
		// For OBO. from http://ontofox.hegroup.org/tutorial/index.php
		prefNameProps.add(df.getOWLAnnotationProperty(IRI.create("http://purl.obolibrary.org/obo/IAO_0000111")));

		synonymProps.add(df.getOWLAnnotationProperty(IRI.create("http://www.w3.org/2004/02/skos/core#altLabel")));
		synonymProps.add(df
				.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasExactSynonym")));
		// For OBO. from http://ontofox.hegroup.org/tutorial/index.php
		synonymProps.add(df.getOWLAnnotationProperty(IRI.create("http://purl.obolibrary.org/obo/IAO_0000118")));

		definitionProps.add(df.getOWLAnnotationProperty(IRI.create("http://www.w3.org/2004/02/skos/core#definition")));
		// For OBO.
		definitionProps.add(
				df.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasDefinition")));
		// For OBO.
		definitionProps.add(df.getOWLAnnotationProperty(IRI.create("http://purl.obolibrary.org/obo/IAO_0000115")));

		obsoleteProps.add(df.getOWLDeprecated());
	}
}
