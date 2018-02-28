package de.julielab.bioportal.ontologies;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import de.julielab.java.utilities.FileUtilities;

public class OntologyLoader {

	private static final Logger log = LoggerFactory.getLogger(OntologyLoader.class);

	private OWLOntologyManager ontologyManager;

	public OntologyLoader() {
		this.ontologyManager = OWLManager.createOWLOntologyManager();
		OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
		config = config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

		ontologyManager.setOntologyLoaderConfiguration(config);
		ontologyManager.addMissingImportListener(event -> log.warn(
				"An exception concerning the ontology import of {} was thrown: {}; Extracted class names will not include classes from that ontology.",
				event.getImportedOntologyURI(), event.getCreationException().getMessage()));
	}

	public OWLOntologyManager getOntologyManager() {
		return ontologyManager;
	}

	public OWLOntology loadOntology(InputStream is) throws OWLOntologyCreationException {
		return ontologyManager.loadOntologyFromOntologyDocument(is);
	}

	public File getMainOntologyFile(File directory) throws IOException {
		if (!directory.isDirectory())
			throw new IllegalArgumentException(directory.getAbsolutePath() + " is not a directory.");

		File downloadFileNameFile = new File(
				directory.getAbsolutePath() + File.separator + BioPortalToolConstants.DOWNLOAD_FILENAME);
		String lcdfn = Files.toString(downloadFileNameFile, Charset.forName("UTF-8")).toLowerCase();
		final String noextension = lcdfn.substring(0, lcdfn.indexOf('.'));
		File[] files = directory.listFiles(f -> f.getName().toLowerCase().startsWith(noextension));
		if (files.length == 1)
			return files[0];
		else
			throw new FileNotFoundException("The main file to load from directory " + directory.getAbsolutePath()
					+ " could not be identified. There were " + files.length + " candidates: "
					+ Stream.of(files).map(f -> f.getAbsolutePath()).collect(Collectors.joining(", ")));
	}

	public OWLOntology loadOntology(File file) throws OWLOntologyCreationException {
		if (file.isDirectory()) {
			// Using the auto IRI mapper will cause the loading of local files
			// over remote files, if available. We set it to the ontology
			// directory where all relevant files should reside.
			// If not given, it might either happen that (slow) downloads from
			// the internet occur or, if those are not available, they are just
			// dropped and the ontology has less classes than it should.
			AutoIRIMapper autoIRIMapper = new AutoIRIMapper(file, true);
			ontologyManager.getIRIMappers().add(autoIRIMapper);

			try {
				loadOntology(getMainOntologyFile(file));
			} catch (IOException e) {
				throw new OWLOntologyCreationException(e);
			} finally {
				// remove the IRI mapper to reset the state of the
				// ontologymanager
				ontologyManager.getIRIMappers().remove(autoIRIMapper);
			}
		}
		OWLOntology o;
		try {
			log.debug("Loading ontology file {}", file);
			o = loadOntology(FileUtilities.getInputStreamFromFile(file));
		} catch (IOException e) {
			throw new OWLOntologyCreationException(e);
		}
		return o;
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
