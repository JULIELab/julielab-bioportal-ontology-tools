package de.julielab.bioportal.ontologies.data;

import java.util.Date;

import com.google.gson.annotations.SerializedName;

public class Submission {
	public String hompepage;
	public String hasOntologyLanguage;
	public Date released;
	public Date creationDate;
	public String documentation;
	public String publication;
	public String version;
	public String description;
	public String status;
	@SerializedName("@id")
	public String id;

	@Override
	public String toString() {
		return "Submission [id=" + id + ", prefLabelProperty=" + prefLabelProperty + ", synonymProperty="
				+ synonymProperty + "]";
	}

	public String prefLabelProperty;
	public String synonymProperty;
	public String definitionProperty;
	public String obsoleteProperty;

	public boolean isPrefLabelPropertyDefined() {
		return prefLabelProperty != null;
	}

	public boolean isSynonymsPropertyDefined() {
		return synonymProperty != null;
	}

	public boolean isDefinitionPropertyDefined() {
		return definitionProperty != null;
	}

	public boolean isObsoletePropertyDefined() {
		return obsoleteProperty != null;
	}
}
