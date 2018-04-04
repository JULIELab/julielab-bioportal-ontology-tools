package de.julielab.bioportal.ontologies.apps;

import static de.julielab.java.utilities.CLIInteractionUtilities.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import org.apache.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.bioportal.util.BioPortalOntologyToolsException;

public class BioPortalTools {
	
	private static Logger log = LoggerFactory.getLogger(BioPortalTools.class);

	public static void main(String[] args)  {
		String task = args.length > 0 ? args[0] : null;
		if (args.length == 0) {
			System.out.println(
					"Please select a task (you may also directly specify the given shortcut on the command line to skip this dialog):\n");
			System.out.println("1. Download ontologies from BioPortal (-do)");
			System.out.println("2. Extract class information from downloaded ontologies (-eci)");
			System.out.println("3. Download ontology mapping from BioPortal (-dm)");
			System.out.println("4. Exit");
			while (task == null) {
				try {
					String line = readLineFromStdIn();
					switch (line) {
					case "1":
					case "1.":
						task = "do";
						break;
					case "2":
					case "2.":
						task = "eci";
						break;
					case "3":
					case "3.":
						task = "dm";
						break;
					case "4":
					case "4.":
						System.exit(0);
					}
				} catch (IOException e) {
					log.error("Reading input from standard input failed. Stack trace follows.", e);
				}
			}
		}
		if (task.startsWith("-"))
			task = task.substring(1);
		String[] argsForApp = args.length > 1 ? IntStream.range(1, args.length).mapToObj(i -> args[i]).toArray(i -> new String[args.length-1]) : new String[0];
		try {
			switch (task) {
			case "do":
				OntologyDownloadApplication
						.main(argsForApp);
				break;
			case "eci":
				NameExtractorApplication.main(argsForApp);
				break;
			case "dm":
				MappingDownloadApplication.main(argsForApp);
				break;
			}
		} catch (ParseException | IOException | BioPortalOntologyToolsException
				| InterruptedException | ExecutionException e) {
			log.error("Executing task " + task + " failed. Exception stacktrace follows.", e);
		}
	}

}
