package de.julielab.bioportal.ontologies;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.julielab.bioportal.ontologies.data.OntologyGroup;
import de.julielab.bioportal.ontologies.data.OntologyMetaData;
import de.julielab.bioportal.ontologies.data.OntologyMetric;
import de.julielab.bioportal.util.BioPortalOntologyToolsException;

public class OntologyListRetriver {
	private static final Logger log = LoggerFactory.getLogger(OntologyListRetriver.class);
	private HttpHandler httpHandler;
	private Gson gson;

	private static final String metaDataInclude = "name,acronym,group,ontologyType&no_context=true";

	public OntologyListRetriver(HttpHandler httpHandler) {
		this.httpHandler = httpHandler;
		this.gson = new Gson();
	}

	public List<OntologyMetaData> getOntologiesMetaData(File outputFile, Set<String> ontologiesToDownload)
			throws ParseException, IOException, BioPortalOntologyToolsException {
		List<OntologyMetaData> effectiveOntologiesMetaData = new ArrayList<OntologyMetaData>();
		Type fromJsonConversionListType = new TypeToken<List<OntologyMetaData>>() {// against
																					// warning
																					// before
																					// empty
																					// blocks
		}.getType();

		HttpEntity response = httpHandler
				.sendGetRequest("http://data.bioontology.org/ontologies?include=" + metaDataInclude);
		String responseString = EntityUtils.toString(response);
		List<OntologyMetaData> ontologiesMetaData = gson.fromJson(responseString, fromJsonConversionListType);

		// Filter for explicitly requested ontologies.
		for (Iterator<OntologyMetaData> iterator = ontologiesMetaData.iterator(); iterator.hasNext();) {
			OntologyMetaData ontologyMetaData = iterator.next();
			if (null != ontologiesToDownload && ontologiesToDownload.size() > 0
					&& !ontologiesToDownload.contains(ontologyMetaData.acronym))
				continue;
			effectiveOntologiesMetaData.add(ontologyMetaData);
		}

		// Add ontology metrics: How many classes? Maximum number of children?
		// How many classes without a description?
		// etc.
		Map<String, OntologyMetric> ontologyMetrics = getOntologyMetrics();
		for (OntologyMetaData metaData : effectiveOntologiesMetaData) {
			OntologyMetric metric = ontologyMetrics.get(metaData.id);
			metaData.ontologyMetric = metric;
		}

		// Add the group ontologies belong to, if any. For example, some
		// ontologies are from OBO, other from the UMLS
		// etc.
		for (OntologyMetaData metaData : effectiveOntologiesMetaData) {
			for (String groupUri : metaData.group) {
				OntologyGroup ontologyGroup = getOntologyGroup(groupUri);
				metaData.addOntologyGroup(ontologyGroup);
			}
		}

		if (null != outputFile) {
			log.info("Storing the ontology meta data list for {} ontologies to {}.", ontologiesMetaData.size(),
					outputFile);
			try (GZIPOutputStream os = new GZIPOutputStream(new FileOutputStream(outputFile))) {
				IOUtils.writeLines(effectiveOntologiesMetaData, "\n", os, "UTF-8");
			}
		}
		return effectiveOntologiesMetaData;
	}

	private Map<String, OntologyMetric> getOntologyMetrics() throws IOException, BioPortalOntologyToolsException {
		String address = "http://data.bioontology.org/metrics";
		HttpEntity response = httpHandler.sendGetRequest(address);
		String utf8Response = HttpHandler.convertEntityToUTF8String(response);
		Type fromJsonConversionListType = new TypeToken<List<OntologyMetric>>() {//
		}.getType();
		List<OntologyMetric> ontologyMetrics = gson.fromJson(utf8Response, fromJsonConversionListType);
		Map<String, OntologyMetric> metricByOntologyUri = new HashMap<>();
		for (OntologyMetric metric : ontologyMetrics)
			metricByOntologyUri.put(metric.links.ontology, metric);
		return metricByOntologyUri;
	}

	private OntologyGroup getOntologyGroup(String groupUri) throws IOException, BioPortalOntologyToolsException {
		HttpEntity response = httpHandler.sendGetRequest(groupUri);
		String utf8Response = HttpHandler.convertEntityToUTF8String(response);
		return gson.fromJson(utf8Response, OntologyGroup.class);
	}
}
