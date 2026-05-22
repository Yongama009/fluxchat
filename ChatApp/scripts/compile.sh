#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."
javac -d /tmp/fluxchat-classes \
  ChatApp/src/server/store/*.java \
  ChatApp/src/server/*.java \
  ChatApp/src/client/*.java
