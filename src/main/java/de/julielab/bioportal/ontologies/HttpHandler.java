package de.julielab.bioportal.ontologies;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.bioportal.util.ResourceAccessDeniedException;
import de.julielab.bioportal.util.ResourceDownloadException;
import de.julielab.bioportal.util.ResourceNotFoundException;

public class HttpHandler {

	private static final Logger log = LoggerFactory.getLogger(HttpHandler.class);
	/**
	 * The BioPortal API key. Without this key, BioPortal won't serve requests
	 * but instead return an error due to the missing key. A key may be obtained
	 * for free by registering with BioPortal.
	 * 
	 * @see http://bioportal.bioontology.org/
	 */
	private String apiKey;
	private HttpClient client;
	private int maxRetries;
	private int waittime;

	public HttpHandler(String apiKey) {
		this(apiKey, 120000, 3, 1800000);
	}

	/**
	 * 
	 * @param apiKey
	 *            BioPortal API key
	 * @param timeout
	 *            Connection timeout in milliseconds
	 * @param maxRetries
	 *            Numbers of retries if a connection fails
	 * @param waittime
	 *            Time to wait between reconnect retries in milliseconds
	 */
	public HttpHandler(String apiKey, int timeout, int maxRetries, int waittime) {
		this.apiKey = apiKey;
		this.maxRetries = maxRetries;
		this.waittime = waittime;
		RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout).setConnectionRequestTimeout(timeout)
				.setSocketTimeout(timeout).build();
		client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
	}

	public HttpEntity sendGetRequest(HttpGet reusableGet) throws SocketException, SocketTimeoutException,
			ResourceNotFoundException, ResourceAccessDeniedException, ResourceDownloadException {
		HttpEntity entity = null;
		try {
			HttpResponse response = client.execute(reusableGet);
			entity = response.getEntity();
			// We take all 200 values with us, because 204 is not really an
			// error. To get specific return codes, see HttpStatus
			// constants.
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode < 300) {
				return entity;
			} else {
				log.error("Error when posting a request to BioPortal Server: {}",
						null != entity ? EntityUtils.toString(entity) : response.getStatusLine());
				if (statusCode == 400)
					throw new ResourceNotFoundException("HTTP status " + statusCode
							+ ": Bad access error, probably is there no such ontology/mapping submission.");
				if (statusCode == 403)
					throw new ResourceAccessDeniedException(
							"HTTP status " + statusCode + ": Access to the requested resource was denied.");
				if (statusCode == 404)
					throw new ResourceNotFoundException("HTTP status " + statusCode + ": Resource not found");
				throw new ResourceDownloadException("HTTP error: " + statusCode);
			}
		} catch (SocketException e) {
			throw e;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (SocketTimeoutException e) {
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return entity;
	}

	public HttpEntity sendGetRequest(String address) throws SocketTimeoutException, ResourceNotFoundException,
			ResourceAccessDeniedException, ResourceDownloadException {
		return sendGetRequest(URI.create(address));
	}

	public HttpEntity sendGetRequest(URI uri) throws ResourceNotFoundException,
			ResourceAccessDeniedException, ResourceDownloadException {
		HttpEntity entity = null;

		HttpGet get = new HttpGet(uri);
		get.setHeader("Authorization", "apikey token=" + apiKey);
		int retries = 0;
		int waittime = this.waittime;
		while (null == entity && retries < maxRetries + 1) {
			try {
				if (log.isTraceEnabled())
					log.trace("Sending request: {}", uri);
				else
					log.debug("Sending request.");
				entity = sendGetRequest(get);
				log.debug("Response received.");
			} catch (ResourceNotFoundException e) {
				throw e;
			} catch (ResourceDownloadException | SocketTimeoutException | SocketException e) {
				if (retries == maxRetries) {
					log.error("{}. retry without success; aborting.", maxRetries);
					throw new ResourceDownloadException(e);
				} else {
					log.error("SocketException ({}) occurred.", e.getMessage());
					log.info("Connection is reset and request tried again.");
					get.reset();
					get = new HttpGet(uri);
					get.setHeader("Authorization", "apikey token=" + apiKey);
					waittime = (int) (waittime * 2.5);
				}
				retries++;
			}
		}
		return entity;
	}

	public static String convertEntityToUTF8String(HttpEntity response)
			throws IOException {
		byte[] responseBytes = EntityUtils.toByteArray(response);
		return new String(responseBytes, "UTF-8");
	}

}
