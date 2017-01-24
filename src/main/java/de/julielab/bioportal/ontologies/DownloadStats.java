package de.julielab.bioportal.ontologies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class DownloadStats {

	private int numSummaries;

	public int getNumSummaries() {
		return numSummaries;
	}

	public void setNumSummaries(int numSummaries) {
		this.numSummaries = numSummaries;
	}

	public List<String> getOntologiesWithoutFile() {
		return ontologiesWithoutFile;
	}

	public List<String> getDeniedOntologies() {
		return deniedOntologies;
	}

	public List<String> getDownloadedOntologies() {
		return downloadedOntologies;
	}

	public List<Pair<String, String>> getOntologiesWithDownloadError() {
		return ontologiesWithDownloadError;
	}

	private List<String> ontologiesWithoutFile = Collections.emptyList();
	private List<String> deniedOntologies = Collections.emptyList();
	private List<String> downloadedOntologies = Collections.emptyList();
	private List<Pair<String, String>> ontologiesWithDownloadError = Collections.emptyList();

	public void addOntologyWithoutFile(String acronym) {
		if (ontologiesWithoutFile.isEmpty()) {
			ontologiesWithoutFile = new ArrayList<>();
		}
		ontologiesWithoutFile.add(acronym);
	}

	public void addDeniedOntology(String acronym) {
		if (deniedOntologies.isEmpty()) {
			deniedOntologies = new ArrayList<>();
		}
		deniedOntologies.add(acronym);
	}

	public void addDownloadedOntology(String acronym) {
		if (downloadedOntologies.isEmpty()) {
			downloadedOntologies = new ArrayList<>();
		}
		downloadedOntologies.add(acronym);
	}

	public void addOntologyWithDownloadError(String acronym, String errorMessage) {
		if (ontologiesWithDownloadError.isEmpty()) {
			ontologiesWithDownloadError = new ArrayList<>();
		}
		ontologiesWithDownloadError.add(new ImmutablePair<String, String>(acronym, errorMessage));
	}

	public String report() {
		StringBuilder sb = new StringBuilder();
		sb.append("Number of successfully downloaded ontologies: " + downloadedOntologies.size() + "\n");
		sb.append("Number of ontologies which were only summaries: " + numSummaries + "\n");
		sb.append("The following ontologies couldn't be downloaded because no file was available:\n");
		for (String ontoWithoutFile : ontologiesWithoutFile) {
			sb.append("\t");
			sb.append(ontoWithoutFile);
			sb.append("\n");
		}
		sb.append("\n");
		sb.append(
				"The following ontologies couldn't be downloaded because at least one resource associated with the ontology was denied access to:\n");
		for (String deniedOntology : deniedOntologies) {
			sb.append("\t");
			sb.append(deniedOntology);
			sb.append("\n");
		}
		if (deniedOntologies.isEmpty())
			sb.append("<none>\n");
		
		sb.append("\n");
		sb.append(
				"The following ontologies couldn't be downloaded because there were errors during download:\n");
		for (Pair<String, String> errorOntology : ontologiesWithDownloadError) {
			sb.append("\t");
			sb.append(errorOntology.getLeft());
			sb.append(": ");
			sb.append(errorOntology.getRight());
			sb.append("\n");
		}
		if (ontologiesWithDownloadError.isEmpty())
			sb.append("<none>\n");
		return sb.toString();
	}

	public void incNumSummaries() {
		++numSummaries;
	}

	public int getNumOntologiesDownloaded() {
		return downloadedOntologies.size();
	}

	public int getNumOntologiesDenied() {
		return deniedOntologies.size();
	}

	public int getNumOntologiesWithoutFile() {
		return ontologiesWithoutFile.size();
	}

	public void removeOntologyWithDownloadError(String acronym) {
		Iterator<Pair<String, String>> iterator = ontologiesWithDownloadError.iterator();
		while (iterator.hasNext()) {
			Pair<java.lang.String, java.lang.String> pair = (Pair<java.lang.String, java.lang.String>) iterator.next();
			if (pair.getLeft().equals(acronym))
				iterator.remove();
		}
	}

}
