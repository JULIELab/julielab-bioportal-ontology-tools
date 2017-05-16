package de.julielab.bioportal.ontologies;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import de.julielab.bioportal.util.BioPortalToolUtils;

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
		if (file.isDirectory()) {
			Map<IRI, File> filesByIri = new HashMap<>();
			Set<IRI> imports = new HashSet<>();
			loadOntologyFromFileOrDirectory(file, filesByIri, imports);
			System.out.println("Entries:");
			filesByIri.entrySet().forEach(System.out::println);
			System.out.println("\nImports:");
			imports.forEach(System.out::println);
			System.out.println("All minus imports:");
			Sets.difference(filesByIri.keySet(), imports).forEach(System.out::println);
			return null;
		}
		OWLOntology o;
		try {
			o = loadOntology(BioPortalToolUtils.getInputStreamFromFile(file));
		} catch (IOException e) {
			throw new OWLOntologyCreationException(e);
		}
		return o;
	}

	private void loadOntologyFromFileOrDirectory(File file, Map<IRI, File> filesByIri, Set<IRI> imports)
			throws OWLOntologyCreationException {
		if (file.isDirectory()) {
			File[] files = file.listFiles(f -> !f.getName().equals(".DS_Store"));
			for (int i = 0; i < files.length; ++i) {
				File f = files[i];
				if (f.isFile()) {
					OWLOntology o = loadOntology(f);
					o.getOntologyID().getOntologyIRI().ifPresent(iri -> filesByIri.put(iri, f));
					o.importsDeclarations().map(imp -> imp.getIRI())
							.forEach(imports::add);
					clearLoadedOntologies();
				} else {
					loadOntologyFromFileOrDirectory(f, filesByIri, imports);
				}
			}

		}
	}

	public void clearLoadedOntologies() {
		ontologyManager.clearOntologies();
	}

	public void removeOntology(OWLOntologyID ontologyID) {
		if (ontologyManager.getOntology(ontologyID) != null) {
			log.debug("Removing ontology with IRI {}, version {} from OWLOntologyManager.",
					ontologyID.getOntologyIRI().toString(), ontologyID.getOntologyIRI());
			ontologyManager.removeOntology(ontologyID);
		}

	}

}
