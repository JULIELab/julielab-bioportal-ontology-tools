package de.julielab.bioportal.ontologies;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Sets;

import de.julielab.bioportal.ontologies.OntologyDownloader;

/**
 * Only used to download test data. You have to insert your BioPortal API key in
 * the code below to let the download run.
 * 
 * @author faessler
 *
 */
@Ignore
public class OntologyDownloaderTest {

	@BeforeClass
	public static void setup() throws IOException {
		File downloadDir = new File("src/test/resources/download-test");
		if (downloadDir.exists())
			FileUtils.deleteDirectory(downloadDir);
	}

	@Test
	public void testDownload() throws Exception {
		OntologyDownloader downloader = new OntologyDownloader("YOUR API KEY");
		downloader.downloadOntologies(new File("src/test/resources/download-test/ontologies"),
				new File("src/test/resources/download-test/info"), Sets.newHashSet("GRO"));
	}
}
