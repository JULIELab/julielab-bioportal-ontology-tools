package de.julielab.bioportal.util;

public class OntologyFileNotAvailableException extends BioPortalOntologyToolsException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8316320784957156505L;

	public OntologyFileNotAvailableException() {
		super();
	}

	public OntologyFileNotAvailableException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public OntologyFileNotAvailableException(String message, Throwable cause) {
		super(message, cause);
	}

	public OntologyFileNotAvailableException(String message) {
		super(message);
	}

	public OntologyFileNotAvailableException(Throwable cause) {
		super(cause);
	}

}
