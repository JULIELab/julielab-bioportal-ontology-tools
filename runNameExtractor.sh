#!/bin/bash
ASSEMBLY=`ls target/julielab-*-assembly*jar`
echo "Using file $ASSEMBLY as application archive"
$JAVA_8/bin/java -cp $ASSEMBLY de.julielab.bioportal.ontologies.apps.NameExtractor $*
