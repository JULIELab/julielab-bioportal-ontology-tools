package de.julielab.bioportal.ontologies.data;

import java.util.ArrayList;
import java.util.List;

public class OntologyClassParents {
	public List<String> parents;

	public void addParent(String synonym) {
		if (null == parents)
			parents = new ArrayList<String>();
		parents.add(synonym);
	}
}