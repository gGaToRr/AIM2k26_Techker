package test.framework;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

// Exécuteur de tests unitaire TDD avec affichage ANSI et rapport détaillé
public class TestRunner {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_BOLD = "\u001B[1m";

    private final List<Class<?>> testClasses = new ArrayList<>();

    public void registerTestClass(Class<?> testClass) {
        testClasses.add(testClass);
    }

    public boolean runAll() {
        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;
        long startTime = System.currentTimeMillis();

        System.out.println(ANSI_BOLD + ANSI_CYAN + "==========================================================" + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_CYAN + "       EXÉCUTION DE LA SUITE DE TESTS TDD (AIM2k26)       " + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_CYAN + "==========================================================" + ANSI_RESET);

        for (Class<?> testClass : testClasses) {
            System.out.println("\n" + ANSI_BOLD + "Classe de Test : " + testClass.getSimpleName() + ANSI_RESET);
            Object testInstance;
            try {
                testInstance = testClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                System.out.println(ANSI_RED + "  Impossible d'instancier " + testClass.getName() + " : " + e.getMessage() + ANSI_RESET);
                failedTests++;
                continue;
            }

            for (Method method : testClass.getDeclaredMethods()) {
                if (method.getName().startsWith("test") && method.getParameterCount() == 0) {
                    totalTests++;
                    String testName = method.getName();
                    try {
                        method.invoke(testInstance);
                        passedTests++;
                        System.out.println("  " + ANSI_GREEN + "✔ [PASS]" + ANSI_RESET + " " + testName);
                    } catch (Throwable t) {
                        failedTests++;
                        Throwable cause = t.getCause() != null ? t.getCause() : t;
                        System.out.println("  " + ANSI_RED + "✘ [FAIL]" + ANSI_RESET + " " + testName);
                        System.out.println("     " + ANSI_YELLOW + "Raison : " + cause.getMessage() + ANSI_RESET);
                    }
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("\n" + ANSI_BOLD + ANSI_CYAN + "----------------------------------------------------------" + ANSI_RESET);
        System.out.println(ANSI_BOLD + "RÉSULTATS DES TESTS :" + ANSI_RESET);
        System.out.println("  Total exécutés : " + totalTests);
        System.out.println("  " + ANSI_GREEN + "Réussis        : " + passedTests + ANSI_RESET);
        if (failedTests > 0) {
            System.out.println("  " + ANSI_RED + "Échoués        : " + failedTests + ANSI_RESET);
        } else {
            System.out.println("  " + ANSI_GREEN + "Échoués        : 0" + ANSI_RESET);
        }
        System.out.println("  Durée totale   : " + duration + " ms");
        System.out.println(ANSI_BOLD + ANSI_CYAN + "==========================================================" + ANSI_RESET);

        return failedTests == 0;
    }
}
