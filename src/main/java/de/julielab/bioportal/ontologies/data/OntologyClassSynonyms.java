package de.julielab.bioportal.ontologies.data;

import java.util.ArrayList;
import java.util.List;

public class OntologyClassSynonyms {
	public static final OntologyClassSynonyms EMPTY_SYNONYMS = new OntologyClassSynonyms(); 
	
	public List<String> synonyms;

	public void addSynonym(String synonym) {
		if (null == synonyms)
			synonyms = new ArrayList<String>();
		synonyms.add(synonym);
	}
	
	
}