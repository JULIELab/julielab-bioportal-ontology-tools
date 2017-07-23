package de.julielab.bioportal.ontologies;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import de.julielab.bioportal.ontologies.data.OntologyMetaData;
import de.julielab.bioportal.ontologies.data.Submission;
import de.julielab.bioportal.util.BioPortalOntologyToolsException;
import de.julielab.bioportal.util.BioPortalToolUtils;
import de.julielab.bioportal.util.OntologyFileNotAvailableException;
import de.julielab.bioportal.util.ResourceAccessDeniedException;
import de.julielab.bioportal.util.ResourceDownloadException;
import de.julielab.bioportal.util.ResourceNotFoundException;

public class OntologyDownloader {

	private static final Logger log = LoggerFactory.getLogger(OntologyDownloader.class);

	public static final String ONTOLOGY_LIST = "ONTOLOGY_LIST.gz";

	private static final String submissionInclude = "submissionId,ontology,released,contact,status,description,creationDate,version,publication,hasOntologyLanguage,homepage,documentation,synonymProperty,definitionProperty,prefLabelProperty,obsoleteProperty";
	/**
	 * A format string pointing to the latest submission of the ontology which
	 * acronym is given as the only parameter.
	 */
	private static final String latestSubmissionEndpointFmtString = "http://data.bioontology.org/ontologies/%s/latest_submission?include="
			+ submissionInclude;

	private HttpHandler httpHandler;

	private Gson gson;

	private OntologyListRetriver ontologyListRetriver;

	private String apiKey;

	private ExecutorService executor;

	private Matcher filenameHeaderMatcher;

	public OntologyDownloader(String apiKey) {
		this.apiKey = apiKey;
		httpHandler = new HttpHandler(apiKey);
		ontologyListRetriver = new OntologyListRetriver(httpHandler);
		this.gson = BioPortalToolUtils.getGson();
		executor = Executors.newFixedThreadPool(20);
		filenameHeaderMatcher = Pattern.compile(".*filename=\"([^\"]+)\".*").matcher("");
	}

	public DownloadStats downloadOntologies(File ontologyDataDir, File ontologyInfoDir,
			Set<String> ontologiesToDownload) throws ParseException, IOException, BioPortalOntologyToolsException,
			InterruptedException, ExecutionException {
		DownloadStats downloadStats = new DownloadStats();

		if (!ontologyDataDir.exists()) {
			log.info("Ontology data directory {} does not exist and is created.", ontologyDataDir);
			ontologyDataDir.mkdirs();
		}

		if (!ontologyInfoDir.exists()) {
			log.info("Ontology information directory {} does not exist and is created.", ontologyInfoDir);
			ontologyInfoDir.mkdirs();
		}

		log.info("Downloading BioPortal ontologies to {}. {}.", ontologyDataDir,
				ontologiesToDownload.size() == 0 ? "No restrictions on downloaded ontologies imposed"
						: "Ontology download is restricted to the ontologies with the following acronyms: "
								+ StringUtils.join(ontologiesToDownload, ", "));
		List<OntologyMetaData> ontologiesMetaData = ontologyListRetriver.getOntologiesMetaData(
				new File(ontologyInfoDir.getAbsolutePath() + File.separator + ONTOLOGY_LIST), ontologiesToDownload);

		List<Pair<Future<OntologyMetaData>, DownloadWorker>> ontologiesWithDownloadIssues = new ArrayList<>();
		for (OntologyMetaData metaData : ontologiesMetaData) {

			if (metaData.summaryOnly) {
				log.debug("Skipping ontology {} because it is just a summary.", metaData.acronym);
				downloadStats.incNumSummaries();
				continue;
			}
			if (!metaData.type.equals("http://data.bioontology.org/metadata/Ontology")) {
				log.warn("Ontology {} has type {}", metaData.acronym, metaData.type);
			}

			DownloadWorker worker = new DownloadWorker(metaData, ontologyDataDir, ontologyInfoDir, downloadStats);
			try {
				worker.download();
			} catch (JsonSyntaxException | IOException e) {
				e.printStackTrace();
			} catch (ResourceDownloadException e) {
				log.info(
						"Couldn't download all data for {} at first try. Pushing ontology to background and continuing with next ontology on the main thread.",
						metaData.acronym);
				Future<OntologyMetaData> future = executor.submit(worker);
				ontologiesWithDownloadIssues.add(
						new ImmutablePair<Future<OntologyMetaData>, OntologyDownloader.DownloadWorker>(future, worker));
			}
		}
		log.info(
				"Finished downloading ontologies in main thread. {} ontologies had issues during download. Waiting for them to finish.",
				ontologiesWithDownloadIssues.size());
		Iterator<Pair<Future<OntologyMetaData>, DownloadWorker>> futureIt = ontologiesWithDownloadIssues.iterator();
		while (futureIt.hasNext()) {
			Pair<Future<OntologyMetaData>, DownloadWorker> futurePair = (Pair<Future<OntologyMetaData>, DownloadWorker>) futureIt
					.next();
			Future<OntologyMetaData> future = futurePair.getLeft();
			DownloadWorker worker = futurePair.getRight();
			log.info("Waiting for ontology {}", worker.getOntologyMetaData().acronym);
			OntologyMetaData metaData = future.get();

			if (metaData != null) {
				log.info("Ontology {} could be downloaded successfully. Removing from list of download issues.");
				downloadStats.removeOntologyWithDownloadError(metaData.acronym);
				futureIt.remove();
			} else {
				// if the metaData is null it means the ontology failed to
				// download
				log.info("Ontology {} failed to download.", worker.getOntologyMetaData().acronym);
				worker.removeOntologyFiles();
			}
		}

		return downloadStats;
	}

	private String downloadInfoForOntology(String address, File destFile, OntologyMetaData metaData, String infoType)
			throws ResourceAccessDeniedException, ResourceNotFoundException, ResourceDownloadException, ParseException,
			IOException {
		if (destFile.exists() && destFile.length() > 0) {
			log.info("The file {} exists and is not empty. It is kept, download of the file is skipped.", destFile);
			return IOUtils.toString(BioPortalToolUtils.getInputStreamFromFile(destFile), Charset.forName("UTF-8"));
		}
		try {
			log.debug("Fetching {} from BioPortal for {}", infoType, metaData.acronym);
			HttpEntity propertiesResponse = httpHandler.sendGetRequest(address);
			String infoString = EntityUtils.toString(propertiesResponse);
			try (Writer w = BioPortalToolUtils.getWriterToFile(destFile)) {
				w.write(infoString);
			}
			return infoString;
		} catch (ResourceDownloadException e) {
			log.error("Error occured when trying to retrieve latest " + infoType + " of ontology " + metaData.acronym
					+ ":", e);
			throw e;
		}
	}

	private class DownloadWorker implements Callable<OntologyMetaData> {

		private File submissionFile;
		private File submissionsFile;
		private File projectsFile;
		private File analyticsFile;
		private OntologyMetaData metaData;
		private File ontologyDataDir;
		private DownloadStats downloadStats;
		private File metaDataFile;

		public DownloadWorker(OntologyMetaData metaData, File ontologyDataDir, File ontologyInfoDir,
				DownloadStats downloadStats) {
			this.metaData = metaData;
			this.ontologyDataDir = ontologyDataDir;
			this.downloadStats = downloadStats;
			this.metaDataFile = new File(ontologyInfoDir.getAbsolutePath() + File.separator + metaData.acronym
					+ BioPortalToolConstants.METADATA_EXT + ".gz");
			this.submissionFile = new File(ontologyInfoDir.getAbsolutePath() + File.separator + metaData.acronym
					+ BioPortalToolConstants.SUBMISSION_EXT + ".gz");
			this.submissionsFile = new File(ontologyInfoDir.getAbsolutePath() + File.separator + metaData.acronym
					+ BioPortalToolConstants.SUBMISSIONS_EXT + ".gz");
			this.projectsFile = new File(ontologyInfoDir.getAbsolutePath() + File.separator + metaData.acronym
					+ BioPortalToolConstants.PROJECTS_EXT + ".gz");
			this.analyticsFile = new File(ontologyInfoDir.getAbsolutePath() + File.separator + metaData.acronym
					+ BioPortalToolConstants.ANALYTICS_EXT + ".gz");
		}

		public OntologyMetaData getOntologyMetaData() {
			return metaData;
		}

		public void download() throws JsonSyntaxException, IOException, ResourceDownloadException, ParseException {
			try {
				if (!metaDataFile.exists())
					try (Writer w = BioPortalToolUtils.getWriterToFile(metaDataFile)) {
						w.write(gson.toJson(metaData));
					}
				else
					log.info("Meta data file {} already exist and is not overwritten", metaDataFile);
				String submission = downloadInfoForOntology(
						String.format(latestSubmissionEndpointFmtString, metaData.acronym), submissionFile, metaData,
						"latest submission");
				downloadInfoForOntology(metaData.links.submissions.toString(), submissionsFile, metaData,
						"submissions");
				downloadInfoForOntology(metaData.links.projects.toString(), projectsFile, metaData, "projects");

				downloadInfoForOntology(metaData.links.analytics.toString(), analyticsFile, metaData, "analytics");

				downloadOntologyFile(ontologyDataDir, metaData, gson.fromJson(submission, Submission.class));
				downloadStats.addDownloadedOntology(metaData.acronym);
			} catch (OntologyFileNotAvailableException e) {
				log.warn(
						"Ontology {} could not be downloaded because no file is available for download. Deleting info files for this ontology.",
						metaData.acronym);
				downloadStats.addOntologyWithoutFile(metaData.acronym);
				removeOntologyFiles();

			} catch (ResourceAccessDeniedException e) {
				log.warn("Ontology {} could not be downloaded because the server rejected access: {}", metaData.acronym,
						e.getMessage());
				downloadStats.addDeniedOntology(metaData.acronym);
				removeOntologyFiles();

			} catch (ResourceNotFoundException e) {
				log.warn("Resource not found for ontology {}: {}. Ontology is skipped.", metaData.acronym,
						e.getMessage());
				downloadStats.addOntologyWithDownloadError(metaData.acronym, e.getMessage());
				removeOntologyFiles();
			}
		}

		/**
		 * Deletes the information files for the ontology this worker is
		 * responsible for.
		 */
		private void removeOntologyFiles() {
			log.info("Deleting info files for ontology {}", metaData.acronym);
			if (metaDataFile.exists())
				metaDataFile.delete();
			if (submissionFile.exists())
				submissionFile.delete();
			if (submissionsFile.exists())
				submissionsFile.delete();
			if (projectsFile.exists())
				projectsFile.delete();
			if (analyticsFile.exists())
				analyticsFile.delete();
		}

		@Override
		public OntologyMetaData call() throws Exception {
			int maxRetries = 10;

			int retries = 0;
			while (retries < maxRetries) {
				try {
					download();
					// When there was no exception, the break is executed and we
					// are finished
					return metaData;
				} catch (IOException | ResourceDownloadException e) {
					try {

						// HTTP 504 = gateway timeout
						// try again
						if (e.getMessage().contains("504")) {
							log.info("{}: Error was a gateway timeout. Waiting an hour and then retry.",
									metaData.acronym);
							Thread.sleep(3600000);
							++retries;
						} else {
							log.info("{}: Server error occured: {}. Waiting an hour and then retry", metaData.acronym,
									e.getMessage());
							Thread.sleep(3600000);
							++retries;
						}
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}

				} catch (JsonSyntaxException | ParseException e) {
					e.printStackTrace();
					break;
				}
			}
			if (retries == maxRetries)
				log.error("Could not download complete data for ontology {} after {} retries. Aborting.",
						metaData.acronym, retries);
			return null;
		}

	}

	private void downloadOntologyFile(File ontologyDataDir, OntologyMetaData ontoInf, Submission submission)
			throws IOException, OntologyFileNotAvailableException {
		// get file name
		String ontoLanguage = "unknown";
		if (submission.hasOntologyLanguage != null)
			ontoLanguage = submission.hasOntologyLanguage.toLowerCase();
		String fileName = ontoInf.acronym + "." + ontoLanguage + ".gz";
		File ontologyFile = new File(ontologyDataDir.getAbsolutePath() + File.separator + fileName);
		if (ontologyFile.exists() && ontologyFile.length() > 0) {
			log.info("Ontology file {} exists and is not empty. File is kept and not downloaded again.",
					ontologyFile.getAbsolutePath());
			return;
		}
		// some ontologies actually consist of multiple files, those are stored
		// in a directory of their own
		File ontologyDir = new File(ontologyDataDir.getAbsolutePath() + File.separator + ontoInf.acronym);
		if (ontologyDir.exists()) {
			String[] list = ontologyDir.list((dir, name) -> !name.equals(".DS_Store"));
			if (null != list && list.length != 0)
				log.info(
						"Ontology directory {} exists and is not empty. The directory and its files are kept and not downloaded again.",
						ontologyDir);
			return;
		}

		// establish connection
		log.debug("Downloading ontology {} from {}.", ontoInf.acronym, ontoInf.links.download);
		HttpURLConnection conn = (HttpURLConnection) ontoInf.links.download.openConnection();
		conn.setRequestProperty("Authorization", "apikey token=" + apiKey);
		String mimeType = conn.getHeaderField(HttpHeaders.CONTENT_DISPOSITION);

		String downloadFileName = null;
		if (mimeType != null) {
			filenameHeaderMatcher.reset(mimeType);
			if (filenameHeaderMatcher.find())
				downloadFileName = filenameHeaderMatcher.group(1);
		}

		// if the request was successful, ...
		int responseCode = conn.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {

			// get input stream
			try (InputStream is = conn.getInputStream()) {

				// Sometimes, the ontology files are in zip format. Since we
				// store in GZIP anyway, we uncompress the stream first.
				if (null != downloadFileName && downloadFileName.toLowerCase().endsWith(".zip")) {
					log.info(
							"Download for ontology {} is a zip file. Storing the contents of the archive in directory {}.",
							ontoInf.acronym, ontologyDir);
					new File(ontologyDataDir.getAbsolutePath() + File.separator + ontoInf.acronym).mkdir();
					ZipInputStream zipStream = new ZipInputStream(is, Charset.forName("UTF-8"));
					ZipEntry entry = zipStream.getNextEntry();
					int numEntries = 0;
					File outputFile = null;
					while (entry != null) {
						++numEntries;
						String filename = entry.getName();

						outputFile = new File(ontologyDataDir.getAbsolutePath() + File.separator + ontoInf.acronym
								+ File.separator + filename + ".gz");
						if (!outputFile.getAbsoluteFile().getParentFile().exists())
							outputFile.getAbsoluteFile().getParentFile().mkdirs();
						writeStreamToFile(zipStream, outputFile);
						entry = zipStream.getNextEntry();
					}
					if (numEntries == 1) {
						log.info(
								"Downloaded ZIP file {} for ontology {} only contained a single entry. Moving it to {}",
								new Object[] { downloadFileName, ontoInf.acronym, ontologyFile });
						Files.move(outputFile.toPath(), ontologyFile.toPath());
						Files.delete(ontologyDir.toPath());
					} else {
						Files.write(
								Paths.get(ontologyDataDir.getAbsolutePath() + File.separator + ontoInf.acronym
										+ File.separator + BioPortalToolConstants.DOWNLOAD_FILENAME),
								downloadFileName.getBytes());
					}
				} else {
					writeStreamToFile(is, ontologyFile);
				}
			}
		} else {
			// okay, there could possibly another reason for this error but for
			// now only the message that there is nothing to download has
			// occurred
			throw new OntologyFileNotAvailableException("Ontology with acronym " + ontoInf.acronym
					+ " does not yet have a file ready for download (error message: "
					+ IOUtils.toString(conn.getErrorStream(), Charset.forName("UTF-8")) + ").");
		}

		conn.disconnect();
	}

	private void writeStreamToFile(InputStream is, File outputFile) throws FileNotFoundException, IOException {
		// write to file
		try (OutputStream os = new GZIPOutputStream(new FileOutputStream(outputFile))) {
			int bytesRead = -1;
			byte[] buffer = new byte[4096];
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
		}
	}
}
