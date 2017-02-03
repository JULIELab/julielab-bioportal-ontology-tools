package de.julielab.bioportal.ontologies.data;

import java.util.ArrayList;
import java.util.List;

public class OntologyClassParents {
	public static final OntologyClassParents EMPTY_PARENTS = new OntologyClassParents();
	
	public List<String> parents;

	public void addParent(String synonym) {
		if (null == parents)
			parents = new ArrayList<String>();
		parents.add(synonym);
	}
}