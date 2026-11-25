#!/usr/bin/env bash
set -euo pipefail

if ! command -v java >/dev/null 2>&1; then
  echo 'ERROR: Java is not installed. Install JDK 8.' >&2
  exit 1
fi
JAVA_MAJOR="$(java -version 2>&1 | awk -F '[".]' '/version/ { if ($2 == "1") print $3; else print $2; exit }')"
if [[ "$JAVA_MAJOR" != "8" ]]; then
  echo "ERROR: Forge 1.12.2 / ForgeGradle 2.3 needs JDK 8 for this project. Detected Java ${JAVA_MAJOR}." >&2
  exit 1
fi
if ! command -v gradle >/dev/null 2>&1; then
  echo 'ERROR: Gradle 4.10.3 is required. Install it, then run this script again.' >&2
  exit 1
fi
gradle --version | grep -q 'Gradle 4.10.3' || {
  echo 'ERROR: Use Gradle 4.10.3 with JDK 8.' >&2
  exit 1
}
gradle setupDecompWorkspace build
