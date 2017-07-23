package de.julielab.bioportal.ontologies.data;

import com.google.gson.annotations.SerializedName;

/**
 * This class is only used for (de-)serialization to and from JSON with the classes {@link OntologyClassMapping} and -
 * as long as necessary - {@link OntologyClassMappingAutomaticProcess}. This class denotes one argument in a BioPortal
 * mapping definition.
 * 
 * @author faessler
 * 
 */
public class MappedClass {
	@SerializedName("@id")
	public String id;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MappedClass other = (MappedClass) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}