package de.julielab.bioportal.ontologies.data;

import java.util.List;

public class OntologyMetric {
	
	public List<String> submission;
	public String created;
	public int classes;
	public int individuals;
	public int properties;
	public int maxDepth;
	public int maxChildCount;
	public int averageChildCount;
	public int classesWithOneChild;
	public int classesWithMoreThan25Children;
	public int classesWithNoDefinition;
	public OntologyMetricLinks links;

	@Override
	public String toString() {
		return "OntologyMetric [submission=" + submission + ", created=" + created + ", classes=" + classes
				+ ", individuals=" + individuals + ", properties=" + properties + ", maxDepth=" + maxDepth
				+ ", maxChildCount=" + maxChildCount + ", averageChildCount=" + averageChildCount
				+ ", classesWithOneChild=" + classesWithOneChild + ", classesWithMoreThan25Children="
				+ classesWithMoreThan25Children + ", classesWithNoDefinition=" + classesWithNoDefinition + "]";
	}

}
