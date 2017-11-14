#!/bin/bash

java $JVM_OPTS -cp "target/classes:target/lib/*" de.julielab.bioportal.ontologies.apps.NameExtractorApplication $*
