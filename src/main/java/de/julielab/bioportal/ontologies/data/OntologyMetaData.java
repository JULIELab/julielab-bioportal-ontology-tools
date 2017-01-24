package de.julielab.bioportal.ontologies.data;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class OntologyMetaData {

	@SerializedName("@id")
	public String id;
	public String acronym;
	public String name;
	public List<String> group;
	public boolean summaryOnly;
	@SerializedName("@type")
	public String type;
	public String ontologyType;
	public OntologyLinks links;
	/**
	 * Not actually sent via the BioPortal API. Instead only {@link #group} is sent, an array of URIs pointing to groups
	 * this ontology belongs to. The OntologyGroup objects are then requested via these URIs and may be added here for
	 * storage. To add ontology groups, please refer to {@link #addOntologyGroup(OntologyGroup)}.
	 */
	public List<OntologyGroup> ontologyGroups;
	/**
	 * Not actually sent via the BioPortal API. Instead ontology metrics (number of classes, depth of ontology etc) have
	 * to be requested separately. Then, the correct metric may be added here.
	 */
	public OntologyMetric ontologyMetric;

	public OntologyMetaData(String name, String acronym) {
		this.name = name;
		this.acronym = acronym;
	}

	public void addOntologyGroup(OntologyGroup ontologyGroup) {
		if (null == ontologyGroups)
			ontologyGroups = new ArrayList<>();
		ontologyGroups.add(ontologyGroup);
	}

	static volatile Gson gson = new Gson();
	@Override
	public String toString() {
		return gson.toJson(this);
	}

	public String apiUrl() {
		return id;
	}

	public String bioportalPurl() {
		return "http://purl.bioontology.org/ontology/" + acronym;
	}

}
