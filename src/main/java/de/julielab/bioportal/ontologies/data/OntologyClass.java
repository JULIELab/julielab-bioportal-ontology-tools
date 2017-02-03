package de.julielab.bioportal.ontologies.data;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * This class is modeled after the BioPortal classes API. Not all fields are
 * set.
 * 
 * @author faessler
 *
 */
public class OntologyClass {
	public class OntologyClassLinks {
		String self;
		String children;
		String parents;
		String descendants;
		String ascestors;
		String tree;
		String ui;
	}

	@SerializedName("@id")
	public String id;
	public String prefLabel;
	public OntologyClassSynonyms synonym = OntologyClassSynonyms.EMPTY_SYNONYMS;
	public List<String> definition;
	public OntologyClassParents parents = OntologyClassParents.EMPTY_PARENTS;
	public Boolean obsolete;
	public String notation;
	public List<String> semanticType;
	public List<String> cui;
	public String xref;

	volatile static Gson gson = new Gson();

	public String toString() {
		return gson.toJson(this);
	}
}