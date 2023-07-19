#!/bin/bash
# This script is runs the EclipseHelper annotion processor on the given project

# skip execution if we are in the root dir
if [[ -f "$(pwd)/runEclipseHelper.sh" ]]; then
	echo "skipping execution in the root directory"
	return 0
fi

if [[ -z $JAVA8 ]]; then
	echo "JAVA8 environment variable not set. Please set it to the path of a Java 8 java executable"
	exit 1
fi

if [[ -z $KNIP_EXTERNALS_US ]]; then
	echo "KNIP_EXTERNALS_US environment variable not set. Please set it to the path of the knip-externals update site directory"
	exit 1
fi


outputDir=$1

# find scijava_common.jar
scijavaCommonJar=$(find "$KNIP_EXTERNALS_US" -name 'scijava-common_*.jar' | head -n 1)
$JAVA8 -Dscijava.log.level=debug -classpath "$scijavaCommonJar:$outputDir" org.scijava.annotations.EclipseHelper 
