#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v21 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

function strongEcho {
  echo ""
  echo "================ $1 ================="
}

case "$TARGET" in

CI)
  # temporary draft
  mvn deploy \
    -Prepox,publish-repox \
    -Dmaven.test.redirectTestOutputToFile=false \
    -Dartifactory.user=$REPOX_QA_QUEUE_USERNAME \
    -Dartifactory.password=$REPOX_QA_QUEUE_PASSWORD \
    -s settings-repox.xml \
    -U -B -e -V
  ;;


WEB)
  set +eu
  source ~/.nvm/nvm.sh && nvm install 4
  npm install -g npm@3.5.2
  cd server/sonar-web && npm install && npm test
  ;;

*)
  echo "Unexpected TARGET value: $TARGET"
  exit 1
  ;;

esac
