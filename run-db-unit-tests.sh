#!/bin/sh

set -euo pipefail

ORCHESTRATOR_CONFIG_URL=$1

mvn verify -pl :sonar-db -Dorchestrator.configUrl=$ORCHESTRATOR_CONFIG_URL -B -e -V
