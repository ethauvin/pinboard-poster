#!/bin/bash

#
# Version: 1.0.1
#

# set the examples directories
declare -a dirs=(
    "${PWD##*/}"
    "samples/java"
    "samples/kotlin")
java8=false

###

pwd=$PWD
cyan=$(tput setaf 6)
green=$(tput setaf 2)
red=$(tput setaf 1)
std=$(tput sgr0)

if [ "$java8" = true ]
then
    export JAVA_HOME="$JAVA8_HOME"
    export PATH="$(cygpath "$JAVA_HOME")/bin:$PATH"
fi

updateWrappers() {
    curVer="$(gradle --version | awk '/Gradle/ {print $2}')"
    if [ -d gradle ]; then
        if [ "$curVer" != "$(./gradlew --version | awk '/Gradle/ {print $2}')" ]; then
            gradle -q --console=plain wrapper
            echo -e "        $(./gradlew --version | awk '/Gradle/') ${green}UPDATED${std}"
        else
            echo -e "        Gradle $curVer UP-TO-DATE"
        fi
    fi
}

echo -e "Updating wrappers..."

for d in "${dirs[@]}"; do
    if [ -d "$d" ]; then
        cd "$d" || exit 1
    fi
    echo -e "    ${cyan}${d}${std}"
    updateWrappers
    cd "$pwd"
done
