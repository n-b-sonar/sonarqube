#!/bin/sh

set -euo pipefail

ORCHESTRATOR_CONFIG_URL=$1

cd it
mvn install -Dcategory=Category1 -Dorchestrator.configUrl=$ORCHESTRATOR_CONFIG_URL -B -e -V

