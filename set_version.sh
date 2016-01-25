#!/bin/bash

set -euo pipefail

NEW_VERSION=$1

mvn org.codehaus.mojo:versions-maven-plugin:2.2:set -DnewVersion=$NEW_VERSION
