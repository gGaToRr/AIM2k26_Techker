package test.framework;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
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

            // Une classe partiellement migree perdrait ses cas non annotes en silence :
            // un test qui existe sans jamais s'executer est pire qu'un test rouge.
            List<String> orphelins = detecterCasOrphelins(testClass);
            for (String orphelin : orphelins) {
                totalTests++;
                failedTests++;
                System.out.println("  " + ANSI_RED + "✘ [FAIL]" + ANSI_RESET + " " + orphelin);
                System.out.println("     " + ANSI_YELLOW
                        + "Raison : methode nommee comme un test mais non annotee @Test, dans une classe "
                        + "qui en utilise. Ajoutez @Test, ou renommez-la si ce n'est pas un test."
                        + ANSI_RESET);
            }

            List<Method> casDeTest = collecterCasDeTest(testClass);
            Method avantChaque = trouverHook(testClass, BeforeEach.class, "setUp");
            Method apresChaque = trouverHook(testClass, AfterEach.class, "tearDown");

            for (Method method : casDeTest) {
                totalTests++;
                String testName = method.getName();

                // Une instance neuve par cas de test : l'etat d'un test ne fuit pas dans le suivant
                Object testInstance;
                try {
                    testInstance = testClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    failedTests++;
                    System.out.println("  " + ANSI_RED + "✘ [FAIL]" + ANSI_RESET + " " + testName);
                    System.out.println("     " + ANSI_YELLOW + "Raison : instanciation impossible - " + e.getMessage() + ANSI_RESET);
                    continue;
                }

                try {
                    invoquer(avantChaque, testInstance);
                    method.invoke(testInstance);
                    passedTests++;
                    System.out.println("  " + ANSI_GREEN + "✔ [PASS]" + ANSI_RESET + " " + testName);
                } catch (Throwable t) {
                    failedTests++;
                    Throwable cause = t.getCause() != null ? t.getCause() : t;
                    System.out.println("  " + ANSI_RED + "✘ [FAIL]" + ANSI_RESET + " " + testName);
                    System.out.println("     " + ANSI_YELLOW + "Raison : " + cause.getMessage() + ANSI_RESET);
                } finally {
                    // Le nettoyage doit avoir lieu meme si le test a echoue
                    try {
                        invoquer(apresChaque, testInstance);
                    } catch (Throwable t) {
                        Throwable cause = t.getCause() != null ? t.getCause() : t;
                        System.out.println("     " + ANSI_YELLOW + "tearDown en echec : " + cause.getMessage() + ANSI_RESET);
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

    // Selectionne les cas de test d'une classe.
    //
    // L'annotation @Test fait autorite des qu'elle est presente quelque part dans la classe.
    // A defaut, on retombe sur l'ancienne convention de prefixe, pour qu'une classe de test
    // non encore migree continue de s'executer.
    static List<Method> collecterCasDeTest(Class<?> testClass) {
        List<Method> annotees = new ArrayList<>();
        List<Method> parConvention = new ArrayList<>();

        for (Method method : testClass.getDeclaredMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            if (method.isAnnotationPresent(Test.class)) {
                annotees.add(method);
            } else if (method.getName().startsWith("test")) {
                parConvention.add(method);
            }
        }

        List<Method> retenus = annotees.isEmpty() ? parConvention : annotees;

        // getDeclaredMethods ne garantit aucun ordre : on trie pour que deux executions
        // successives produisent exactement le meme rapport.
        retenus.sort(Comparator.comparing(Method::getName));
        return retenus;
    }

    // Methodes qui ressemblent a des tests mais ne s'executeraient pas, faute
    // d'annotation, dans une classe qui utilise deja @Test.
    static List<String> detecterCasOrphelins(Class<?> testClass) {
        boolean classeAnnotee = false;
        List<String> suspects = new ArrayList<>();

        for (Method method : testClass.getDeclaredMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            if (method.isAnnotationPresent(Test.class)) {
                classeAnnotee = true;
            } else if (method.getName().startsWith("test")
                    && java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                suspects.add(method.getName());
            }
        }

        if (!classeAnnotee) {
            return List.of();
        }
        suspects.sort(Comparator.naturalOrder());
        return suspects;
    }

    // Cherche un hook par annotation, puis par nom conventionnel
    private static Method trouverHook(Class<?> testClass, Class<? extends java.lang.annotation.Annotation> annotation,
                                      String nomConventionnel) {
        for (Method method : testClass.getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && method.isAnnotationPresent(annotation)) {
                method.setAccessible(true);
                return method;
            }
        }
        for (Method method : testClass.getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && method.getName().equals(nomConventionnel)) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static void invoquer(Method hook, Object instance) throws Exception {
        if (hook != null) {
            hook.invoke(instance);
        }
    }
}
