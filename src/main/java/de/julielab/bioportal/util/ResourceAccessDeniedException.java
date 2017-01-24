package de.julielab.bioportal.util;

public class ResourceAccessDeniedException extends BioPortalOntologyToolsException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7021382466035321184L;

	public ResourceAccessDeniedException() {
		super();
	}

	public ResourceAccessDeniedException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ResourceAccessDeniedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResourceAccessDeniedException(String message) {
		super(message);
	}

	public ResourceAccessDeniedException(Throwable cause) {
		super(cause);
	}

}
