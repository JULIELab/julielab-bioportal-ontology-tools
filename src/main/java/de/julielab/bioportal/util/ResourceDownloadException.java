package de.julielab.bioportal.util;

public class ResourceDownloadException extends BioPortalOntologyToolsException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4812828500438177724L;

	public ResourceDownloadException() {
		super();
	}

	public ResourceDownloadException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ResourceDownloadException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResourceDownloadException(Throwable cause) {
		super(cause);
	}

	public ResourceDownloadException(String message) {
		super(message);
	}
}