#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."
ChatApp/scripts/compile.sh
java -cp /tmp/fluxchat-classes client.ChatGUI
