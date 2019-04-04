#!/bin/bash

export JAVA_HOME="$JAVA8_HOME"
export PATH="$(cygpath "$JAVA_HOME")/bin:$PATH"

declare -a dirs=("${PWD##*/}" "samples/java" "samples/kotlin")
pwd=$PWD

updateWrappers() {
    if [ -d gradle ]; then
        gradle -q --console=plain wrapper
        echo -e "    $(./gradlew --version | grep Gradle)"
    fi
    if [ -d kobalt ]; then
        ./kobaltw --update --log 0
        echo -e "    Kobalt $(cut -d "=" -f 2 kobalt/wrapper/kobalt-wrapper.properties)"
    fi
}

color=$(tput setaf 6)
normal=$(tput sgr0)

for d in "${dirs[@]}"; do
    if [ -d "$d" ]; then
        cd "$d" || exit 1
    fi
    echo -e "${color}${d}${normal}"
    updateWrappers
    cd "$pwd"
done
