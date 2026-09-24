# Exemples calibrés — Dépannage & Diagnostic

Format : chaque exemple commence par `## EXEMPLE`, puis `### DEMANDE` et `### REPONSE`.
La réponse doit montrer le **format** attendu, pas traiter le cas de l'utilisateur.

## EXEMPLE

### DEMANDE
Mon appli Java plante avec NullPointerException dans le service de panier quand le client n'est pas connecte.

### REPONSE
**1. Diagnostic & Cause Racine**
`PanierService.total()` appelle `client.getRemise()` sans vérifier que `client` est non nul. En session anonyme, `SessionContext.getClient()` renvoie `null` : l'appel déréférence donc une référence nulle.

**2. Correctif Chirurgical**
```java
public BigDecimal total() {
    BigDecimal brut = lignes.stream()
            .map(Ligne::sousTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    Client client = session.getClient();
    return client == null ? brut : brut.multiply(client.getRemise());
}
```
Seule la lecture de la remise est gardée ; le calcul du brut et la signature sont inchangés.

**3. Test de Non-Régression**
```java
@Test
public void totalSansClientConnecteNAppliquePasDeRemise() {
    PanierService panier = new PanierService(sessionAnonyme());
    panier.ajouter(new Ligne("ref-1", new BigDecimal("10.00")));
    assertEquals(new BigDecimal("10.00"), panier.total());
}
```

## EXEMPLE

### DEMANDE
Le build docker echoue sur la couche npm install avec un code 137.

### REPONSE
**1. Diagnostic & Cause Racine**
Le code 137 correspond à `SIGKILL` (128 + 9) : le processus a été tué par l'OOM killer. `npm install` dépasse la mémoire allouée au démon Docker, et non une erreur de dépendance — le message npm qui précède est une conséquence, pas la cause.

**2. Correctif Chirurgical**
```dockerfile
RUN npm ci --omit=dev --no-audit --fund=false
```
`npm ci` travaille à partir du lockfile sans résoudre l'arbre complet, ce qui réduit fortement le pic mémoire. À défaut, relever la limite du démon à 4 Go.

**3. Test de Non-Régression**
```bash
docker build --memory=2g --no-cache -t app:test .
```
Le build doit aboutir sous une limite explicite de 2 Go.
