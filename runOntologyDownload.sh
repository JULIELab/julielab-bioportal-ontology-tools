#!/bin/bash
ASSEMBLY=`ls target/julielab-*-assembly*jar`
echo "Using file $ASSEMBLY as application archive"
java -cp $ASSEMBLY de.julielab.bioportal.ontologies.apps.OntologyDownloadApplication $*
