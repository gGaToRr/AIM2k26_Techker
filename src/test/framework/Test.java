package test.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Marque une methode comme cas de test.
//
// Remplace la detection par prefixe "test" : une methode utilitaire nommee
// testXxx par inadvertance ne doit pas etre executee comme un test.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Test {
}
