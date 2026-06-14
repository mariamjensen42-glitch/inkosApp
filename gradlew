#!/bin/sh

#
# Gradle start up script for POSIX
#

# Attempt to set APP_HOME
app_path=$0
while
    APP_HOME=${app_path%"${app_path##*/}"}
    [ -h "$app_path" ]
do
    ls=$( ls -ld -- "$app_path" )
    link=${ls#*' -> '}
    case $link in
      /*)   app_path=$link ;;
      *)    app_path=$APP_HOME$link ;;
    esac
done

APP_BASE_NAME=${0##*/}
APP_HOME=$( cd "${APP_HOME:-./}" > /dev/null && pwd -P ) || exit

# Gradle version
GRADLE_VERSION="9.3.1"
GRADLE_DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_HOME="$HOME/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}"

# Download Gradle if not present
if [ ! -d "$GRADLE_HOME" ]; then
    echo "Downloading Gradle ${GRADLE_VERSION}..."
    mkdir -p "$GRADLE_HOME"
    curl -L -o "/tmp/gradle-${GRADLE_VERSION}.zip" "$GRADLE_DIST_URL"
    unzip -o "/tmp/gradle-${GRADLE_VERSION}.zip" -d "$GRADLE_HOME"
    rm "/tmp/gradle-${GRADLE_VERSION}.zip"
fi

# Find the gradle executable
GRADLE_EXEC="$GRADLE_HOME/gradle-${GRADLE_VERSION}/bin/gradle"
if [ ! -x "$GRADLE_EXEC" ]; then
    echo "ERROR: Gradle executable not found at $GRADLE_EXEC"
    exit 1
fi

# Execute Gradle
exec "$GRADLE_EXEC" "$@"
