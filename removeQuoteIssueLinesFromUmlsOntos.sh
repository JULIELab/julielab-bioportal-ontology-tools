#!/bin/bash

# The issue is that in some UMLS files - which have been converted into RDF
# Turtle format by an NCBO tool - there are string values containing the
# character sequence ''' in strings, e.g. chemicals. Those strings are quoted
# using """. Newer versions of RDF parsers have the issue that they close
# """ initiated strings with '''. That leads to errors in this case.
# Only a few UMLS ontologies have isses with that (MESH, LOINC and some others)
# but we will just go over all of them.
for i in ontology-download/ontologies/*umls*; do
	zcat $i | sed -f parserIssueLines.sed | gzip > tmp; mv tmp $i;
done
