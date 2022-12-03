package de.julielab.bioportal.ontologies;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.net.SocketTimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Sets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import de.julielab.bioportal.ontologies.OntologyDownloader;
import de.julielab.bioportal.ontologies.data.OntologyMetaData;
import de.julielab.bioportal.util.ResourceAccessDeniedException;
import de.julielab.bioportal.util.ResourceDownloadException;
import de.julielab.bioportal.util.ResourceNotFoundException;

/**
 * Only used to download test data. You have to insert your BioPortal API key in
 * the code below to let the download run.
 *
 * @author faessler
 */

public class OntologyDownloaderTest {

    @BeforeClass
    public static void setup() throws IOException {
        File downloadDir = new File("src/test/resources/download-test");
        if (downloadDir.exists())
            FileUtils.deleteDirectory(downloadDir);
    }

    @Test(expected = ResourceDownloadException.class)
    public void testSendGetRequestNullMock() throws Exception {
        String dummy = "";
        File dummyFile = new File("dummy");
        OntologyMetaData metadata = new OntologyMetaData("Foobar", "FB");

        //Make private method accessible
        Class<OntologyDownloader> targetClass = OntologyDownloader.class;
        Method method = targetClass.getDeclaredMethod("downloadInfoForOntology",
                dummy.getClass(), dummyFile.getClass(), metadata.getClass(), dummy.getClass());
        method.setAccessible(true);

        //Mock method to be called
        HttpHandler mock = mock(HttpHandler.class);
        when(mock.sendGetRequest("http://foo")).thenThrow(new ResourceDownloadException());

        //Insert HttpHandler mock into OntologyDownloader via reflection
        OntologyDownloader downloader = new OntologyDownloader("API key");
        Class<?> c = downloader.getClass();
        Field handler = c.getDeclaredField("httpHandler");
        handler.setAccessible(true);
        handler.set(downloader, mock);

        //Call method
        try {
            HttpEntity entity = (HttpEntity) method.invoke(downloader, "http://foo", dummyFile, metadata, "Dummy");
        } catch (InvocationTargetException e) {
            String[] cause = e.getCause().toString().split("\\.");
            if (cause[cause.length - 1].equals("ResourceDownloadException")) {
                throw new ResourceDownloadException();
            } else {
                throw e;
            }
        }
    }
}
