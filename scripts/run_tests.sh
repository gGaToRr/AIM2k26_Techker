#!/bin/bash
# ==============================================================================
# Script d'exécution de la suite complète de tests TDD (AIM2k26_Techker)
# ==============================================================================

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR"

echo "🔨 Compilation du projet et des suites de tests..."
mkdir -p bin
javac -sourcepath ".:src" -cp ".:lib/jmustache-1.16.jar" -d bin \
    Main.java \
    src/menu/*.java \
    src/cli/*.java \
    src/nlp/*.java \
    src/gen/*.java \
    src/llm/*.java \
    test/*.java \
    test/framework/*.java \
    test/nlp/*.java \
    test/gen/*.java \
    test/menu/*.java \
    test/cli/*.java \
    test/llm/*.java \
    test/integration/*.java

echo ""
echo "🚀 Lancement de la suite globale AllTestSuite..."
java -cp "bin:lib/jmustache-1.16.jar" test.AllTestSuite
