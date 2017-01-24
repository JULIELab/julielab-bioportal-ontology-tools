package de.julielab.bioportal.ontologies;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.bioportal.ontologies.data.OntologyMetaData;
import de.julielab.bioportal.util.BioPortalOntologyToolsException;
import de.julielab.bioportal.util.BioPortalToolUtils;
import de.julielab.bioportal.util.OntologyClassPropertyExtractionException;

/**
 * To extract information like names, descriptions etc. from BioPortal
 * ontologies, we need to know in which annotations these information are
 * stored. For some properties, i.e. 'preferred label', 'synonyms' etc. the
 * respective BioPortal ontology submission is queried for each ontology. The
 * submission contains meta data about which annotation property contains one of
 * these types of information. The submission data is written to disk for
 * subsequent use.
 * 
 * @see <a href="http://data.bioontology.org/documentation#OntologySubmission">BioPortal API Docs</a>
 * @author faessler
 *
 */
@Deprecated
public class OntologySubmissionRetriever {

	private final static Logger log = LoggerFactory.getLogger(OntologySubmissionRetriever.class);

	private HttpHandler httpHandler;

	private static final String submissionInclude = "submissionId,ontology,released,contact,status,description,creationDate,version,publication,hasOntologyLanguage,homepage,documentation,synonymProperty,definitionProperty,prefLabelProperty,obsoleteProperty";
	/**
	 * A format string pointing to the latest submission of the ontology which
	 * acronym is given as the only parameter.
	 */
	private static final String latestSubmissionEndpointFmtString = "http://data.bioontology.org/ontologies/%s/latest_submission?include="
			+ submissionInclude;

	public OntologySubmissionRetriever(HttpHandler httpHandler) {
		this.httpHandler = httpHandler;
	}

	public void downloadSubmissionForOntology(File submissionsDirectory, OntologyMetaData metaData) throws Exception {
		log.debug("Fetching latest submission from BioPortal");
		int retries = 0;
		while (retries < 10) {
			try {
				log.debug("Trying to download submission for the {}. time", retries + 1);
				HttpEntity propertiesResponse = httpHandler
						.sendGetRequest(String.format(latestSubmissionEndpointFmtString, metaData.acronym));
				String submissionString = EntityUtils.toString(propertiesResponse);
				File submissionFile = new File(submissionsDirectory.getAbsolutePath() + File.separator
						+ metaData.acronym + BioPortalToolConstants.SUBMISSION_EXT);
				log.debug("Writing submission data for {} to {}", metaData.acronym, submissionFile);
				FileUtils.write(submissionFile, submissionString, Charset.forName("UTF-8"), false);

				break;
			} catch (IOException e) {
				log.error(
						"Error occured when trying to retrieve latest submission of ontology " + metaData.acronym + ":",
						e);
				// HTTP 504 = gateway timeout
				// try again
				if (e.getMessage().contains("504")) {
					log.info("Error was a gateway timeout. Waiting an hour and then retry.");
					Thread.sleep(3600000);
					++retries;
				} else {
					log.info("Unrecoverable error, skipping this ontology");
					throw e;
				}
			} catch (Exception e) {
				throw e;
			}
		}

	}

	public void extractPropertiesForOntologies(File ontologiesPath, File submissionsDirectory)
			throws BioPortalOntologyToolsException, IOException {
		if (submissionsDirectory.exists()) {
			log.info("Deleting old submissions directory {}", submissionsDirectory);
			FileUtils.deleteDirectory(submissionsDirectory);
		}
		log.info("Creating submissions director {}", submissionsDirectory);
		submissionsDirectory.mkdirs();
		try {
			File[] ontologyFiles;
			if (ontologiesPath.isFile()) {
				ontologyFiles = new File[] { ontologiesPath };
			} else {
				ontologyFiles = ontologiesPath.listFiles(new FilenameFilter() {

					public boolean accept(File dir, String name) {
						return !name.equals(".DS_Store") && !name.toLowerCase().matches("catalog-v[0-9]+\\.xml");
					}
				});
				for (int i = 0; i < ontologyFiles.length; i++) {
					File ontologyFile = ontologyFiles[i];
					extractPropertiesForOntologies(ontologyFile, submissionsDirectory);
				}
			}

			for (File ontologyFile : ontologyFiles) {
				String ontologyAcronym = BioPortalToolUtils.getAcronymFromFileName(ontologyFile.getName());
				// ontologyAcronym = ontologyAcronym.substring(0,
				// ontologyAcronym.indexOf('.')).toUpperCase();
				log.debug("Fetching properties of ontology {}", ontologyAcronym);
				HttpEntity propertiesResponse = httpHandler
						.sendGetRequest(String.format(latestSubmissionEndpointFmtString, ontologyAcronym));
				String submissionString = EntityUtils.toString(propertiesResponse);
				File submissionFile = new File(submissionsDirectory.getAbsolutePath() + File.separator + ontologyAcronym
						+ BioPortalToolConstants.SUBMISSION_EXT);
				log.debug("Writing submission data for {} to {}", ontologyAcronym, submissionFile);
				FileUtils.write(submissionFile, submissionString, Charset.forName("UTF-8"), false);
			}
		} catch (Exception e) {
			throw new OntologyClassPropertyExtractionException(e);
		}
	}
}
