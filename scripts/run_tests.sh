#!/bin/bash
# ==============================================================================
# Script d'exécution de la suite complète de tests TDD (AIM2k26_Techker)
# ==============================================================================

set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR"

echo "🔨 Compilation du projet et des suites de tests..."
mkdir -p bin
javac -cp ".:lib/jmustache-1.16.jar" -d bin \
    Main.java \
    menu/Menu.java \
    nlp/*.java \
    gen/*.java \
    test/*.java \
    test/framework/*.java \
    test/nlp/*.java \
    test/gen/*.java \
    test/menu/*.java \
    test/integration/*.java

echo ""
echo "🚀 Lancement de la suite globale AllTestSuite..."
java -cp "bin:lib/jmustache-1.16.jar" test.AllTestSuite
