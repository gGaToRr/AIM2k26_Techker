#!/bin/bash
# ==============================================================================
# Script d'exécution de la suite complète de tests TDD (AIM2k26_Techker)
# ==============================================================================

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$DIR"

echo "🔨 Compilation du projet et des suites de tests..."
mkdir -p bin
javac -sourcepath ".:src" -cp ".:src/lib/jmustache-1.16.jar" -d bin \
    Main.java \
    src/menu/*.java \
    src/cli/*.java \
    src/nlp/*.java \
    src/util/*.java \
    src/gen/*.java \
    src/llm/*.java \
    src/test/*.java \
    src/test/framework/*.java \
    src/test/nlp/*.java \
    src/test/gen/*.java \
    src/test/menu/*.java \
    src/test/cli/*.java \
    src/test/llm/*.java \
    src/test/util/*.java \
    src/test/integration/*.java

echo ""
echo "🚀 Lancement de la suite globale AllTestSuite..."
java -cp "bin:src/lib/jmustache-1.16.jar" test.AllTestSuite

