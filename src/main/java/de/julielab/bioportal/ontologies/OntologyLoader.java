package de.julielab.bioportal.ontologies;

import java.io.File;
import java.io.InputStream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OntologyLoader {
	
	private static final Logger log = LoggerFactory.getLogger(OntologyLoader.class);
	
	private OWLOntologyManager ontologyManager;

	public OntologyLoader() {
		this.ontologyManager = OWLManager.createOWLOntologyManager();     
		OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();       
		config = config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT); 

		ontologyManager.setOntologyLoaderConfiguration(config);     
	}

	public OWLOntologyManager getOntologyManager() {
		return ontologyManager;
	}
	
	public OWLOntology loadOntology(InputStream is) throws OWLOntologyCreationException {
		OWLOntology o = ontologyManager.loadOntologyFromOntologyDocument(is);
		return o;
	}
	
	public OWLOntology loadOntology(File file) throws OWLOntologyCreationException {
		OWLOntology o = ontologyManager.loadOntologyFromOntologyDocument(file);
		return o;
	}
	
	public void clearLoadedOntologies() {
		ontologyManager.clearOntologies();
	}
	
	public void removeOntology(OWLOntologyID ontologyID) {
		if (ontologyManager.getOntology(ontologyID) != null) {
			log.debug("Removing ontology with IRI {}, version {} from OWLOntologyManager.", ontologyID.getOntologyIRI().toString(), ontologyID.getOntologyIRI());
			ontologyManager.removeOntology(ontologyID);
		}
		
	}

}
