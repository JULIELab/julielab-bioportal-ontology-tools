package de.julielab.bioportal.ontologies;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import de.julielab.bioportal.ontologies.data.OntologyClassMapping;
import de.julielab.bioportal.ontologies.data.OntologyMetaData;
import de.julielab.bioportal.util.BioPortalOntologyToolsException;
import de.julielab.bioportal.util.BioPortalToolUtils;
import de.julielab.bioportal.util.ResourceNotFoundException;

public class MappingDownloader {

	private static final Logger log = LoggerFactory.getLogger(MappingDownloader.class);
	private static final Logger errors = LoggerFactory.getLogger(MappingDownloader.class.getCanonicalName() + ".downloaderrors");

	private Gson gson;

	private ExecutorService executorService;

	private Pattern jsonSyntaxErrorPattern = Pattern.compile("[A-Za-z _.:]+[0-9]+[A-Za-z ]+([0-9]+)");
	private Matcher jsonSyntaxErrorMatcher = jsonSyntaxErrorPattern.matcher("");

	private HttpHandler httpHandler;

	private OntologyListRetriver ontologyListRetriver;

	static class OntologyMappingsPage {
		class OntolgoyClassesPageLinks {
			String nextPage;
		}

		int page;
		int pageCount;
		OntolgoyClassesPageLinks links;
		List<OntologyClassMapping> collection;
	}

	public MappingDownloader(String apiKey) {
		gson = new Gson();
		httpHandler = new HttpHandler(apiKey);
		ontologyListRetriver = new OntologyListRetriver(httpHandler);
		executorService = Executors.newFixedThreadPool(6);
	}

	public void downloadOntologyMappings(File mappingsDir, Set<String> ontologiesToDownload) throws ParseException,
			IOException, BioPortalOntologyToolsException {
		errors.info("------- Error report for download beginning at " + (new Date())
				+ " ---------\n", "UTF-8", true);
		
		List<OntologyMetaData> ontologiesMetaData = ontologyListRetriver.getOntologiesMetaData(null, ontologiesToDownload);
		removeAlreadyDownloadedButLast(ontologiesMetaData, mappingsDir);
		log.info("Starting the download of mappings for {} ontologies.", ontologiesMetaData.size());
		List<Future<?>> futures = new ArrayList<>();
		for (int i = 0; i < ontologiesMetaData.size(); i++) {
			OntologyMetaData ontologyMetaData = ontologiesMetaData.get(i);
			try {
				log.info("Downloading mappings for ontology {}.", (i + 1) + "/" + ontologiesMetaData.size());
				DownloadWorker downloadWorker = new DownloadWorker(ontologyMetaData, mappingsDir);
				Future<?> future = executorService.submit(downloadWorker);
				futures.add(future);
			}  catch (JsonSyntaxException e) {
				log.warn("Mappings of ontology {} could not be downloaded due to a JSON parsing error: {}",
						ontologyMetaData.acronym, e.getMessage());
			}
		}
		int i = 1;
		for (Future<?> future : futures) {
			try {
				future.get();
				log.info("{} of {} ontology mappings successfully downloaded.", i, futures.size());
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		executorService.shutdown();
	}
	
	private class DownloadWorker implements Runnable {

		private OntologyMetaData ontologyMetaData;
		private File mappingsDir;

		public DownloadWorker(OntologyMetaData ontologyMetaData, File mappingsDir) {
			this.ontologyMetaData = ontologyMetaData;
			this.mappingsDir = mappingsDir;
		}

		@Override
		public void run() {
			try {
				storeOntologyMappings(ontologyMetaData, mappingsDir);
			} catch (ResourceNotFoundException | ParseException | IOException e) {
				log.error("Exception while downloading mappings for ontology " + ontologyMetaData.acronym, e);
			}
		}
		
	}

	private void storeOntologyMappings(OntologyMetaData ontologyMetaData, File ontosDir) throws ParseException,
			IOException, ResourceNotFoundException {
		String includedAttributes = "pagesize=500&no_context=true&no_links=true";
		String mappingsUrl = ontologyMetaData.apiUrl() + "/mappings?" + includedAttributes;
		File mappingsFile =
				new File(ontosDir.getAbsolutePath() + File.separatorChar
						+ ontologyMetaData.acronym
						+ BioPortalToolConstants.MAPPING_EXT
						+ ".gz");
		if (mappingsFile.exists() && mappingsFile.length() != 0) {
			log.info("Mapping file {} already exists and is not empty. Not downloading again mappings for ontology {}", mappingsFile, ontologyMetaData.acronym);
			return;
		}
		try (OutputStream os = BioPortalToolUtils.getOutputStreamToFile(mappingsFile)) {

			log.info("Mappings of ontology {} are being downloaded (API URL: {}).", ontologyMetaData.bioportalPurl(),
					ontologyMetaData.apiUrl());

			List<OntologyClassMapping> mappings = new ArrayList<>();

			String responseString = null;
			try {
				responseString = EntityUtils.toString(httpHandler.sendGetRequest(mappingsUrl));
			} catch (ResourceNotFoundException e) {
				throw new ResourceNotFoundException(ontologyMetaData.name);
			}
			OntologyMappingsPage ontologyMappingsPage = parseResponse(responseString);
			mappings.addAll(ontologyMappingsPage.collection);
			log.info("Page {} of {} has been downloaded successfully for ontology {}.", new Object[] {ontologyMappingsPage.page,
					ontologyMappingsPage.pageCount, ontologyMetaData.acronym});
			while (ontologyMappingsPage.links.nextPage != null && ontologyMappingsPage.page <= ontologyMappingsPage.pageCount) {
				responseString = EntityUtils.toString(httpHandler.sendGetRequest(ontologyMappingsPage.links.nextPage));
				ontologyMappingsPage = parseResponse(responseString);
				if (null != ontologyMappingsPage) {
					mappings.addAll(ontologyMappingsPage.collection);
					if (ontologyMappingsPage.collection.isEmpty())
						log.warn("Page {} of {} was downloaded empty for ontology {}. The response string was: {}",
								new Object[] { ontologyMappingsPage.page, ontologyMappingsPage.pageCount,
										ontologyMetaData.acronym, responseString });
					else
						log.info("Page {} of {} has been downloaded successfully for ontology {}.", new Object[] {ontologyMappingsPage.page,
								ontologyMappingsPage.pageCount, ontologyMetaData.acronym});
				} else {
					log.warn("Current page of ontology \"{}\" could not be downloaded. Skipping this ontology.",
							ontologyMetaData.acronym);
				}
			}
			if (ontologyMappingsPage.page < ontologyMappingsPage.pageCount)
				log.warn("Only {} of {} pages of mappings have been downloaded for ontology {}.", new Object[] {
						ontologyMappingsPage.page, ontologyMappingsPage.pageCount, ontologyMetaData.name });
			IOUtils.write("[", os, "UTF-8");
			for (int i = 0; i < mappings.size(); i++) {
				OntologyClassMapping mapping = mappings.get(i);
				IOUtils.write(mapping.toString(), os, "UTF-8");
				if (i < mappings.size() - 1)
					IOUtils.write(",", os, "UTF-8");
				IOUtils.write("\n", os, "UTF-8");
			}
			IOUtils.write("]", os, "UTF-8");
			log.info("{} mappings of ontology \"{}\" have been downloaded.", mappings.size(), ontologyMetaData.name);
		} catch (Exception e) {
			String msg =
					"File \"" + mappingsFile.getAbsolutePath()
							+ "\" is deleted because the ontology mapping could not be downloaded completely due to error: "
							+ e.getMessage();
			errors.info(msg + "\n", "UTF-8", true);
			log.info(msg);
			mappingsFile.delete();
		}
	}

	private OntologyMappingsPage parseResponse(String responseString) {
		OntologyMappingsPage ontologyMappingsPage = null;
		try {
			ontologyMappingsPage = gson.fromJson(responseString, OntologyMappingsPage.class);
		} catch (JsonSyntaxException e) {
			log.error("Error message: \"{}\".", e.getMessage());
			jsonSyntaxErrorMatcher.reset(e.getMessage());
			if (jsonSyntaxErrorMatcher.matches()) {
				int column = Integer.parseInt(jsonSyntaxErrorMatcher.group(1));
				String snippet =
						responseString.substring(Math.max(0, column - 40),
								Math.min(column + 40, responseString.length()));
				log.error("Error snippet: \"{}\"", snippet);
			} else {
				log.warn("Tried to extract column number from error output but failed (error message was not of expected format).");
			}
			throw e;
		}
		return ontologyMappingsPage;
	}

	private void removeAlreadyDownloadedButLast(List<OntologyMetaData> ontologiesMetaData, File ontosDir) {
		log.info("Removing already downloaded ontology mappings from ToDo list.");
		File[] downloadedOntologies = ontosDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(BioPortalToolConstants.MAPPING_EXT + ".gz");
			}
		});
		if (downloadedOntologies.length == 0) {
			log.info("No already downladed mapping files with extension " + BioPortalToolConstants.MAPPING_EXT + ".gz" + " found.");
			return;
		}

		HashSet<String> ontologyNamesToRemoveFromDownloadList = new HashSet<String>(downloadedOntologies.length);
		for (File f : downloadedOntologies) {
			String filename = f.getName();
			String ontoAcronym = filename.substring(0, filename.length() - (BioPortalToolConstants.MAPPING_EXT + ".gz").length());
			ontologyNamesToRemoveFromDownloadList.add(ontoAcronym);
		}

		List<String> removedOntologies = new ArrayList<String>();
		for (Iterator<OntologyMetaData> it = ontologiesMetaData.iterator(); it.hasNext();) {
			OntologyMetaData meta = it.next();
			if (ontologyNamesToRemoveFromDownloadList.contains(meta.acronym)) {
				it.remove();
				removedOntologies.add(meta.acronym);
			}
		}
		log.info("Removed the following {} ontologies from download-list: {}.", removedOntologies.size(),
				StringUtils.join(removedOntologies, ", "));
	}

	public void shutdown() {
		executorService.shutdown();
	}

}
