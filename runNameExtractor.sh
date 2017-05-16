#!/bin/bash

java -cp "target/classes:target/lib/*" de.julielab.bioportal.ontologies.apps.NameExtractor $*
