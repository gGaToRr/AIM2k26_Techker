package nlp;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Extrait dynamiquement le domaine d'expertise, le sujet pivot et forge un persona d'élite adapté
public class DomainExtractor {

    public record DomainInfo(
            String domainName,
            String extractedTopic,
            String expertPersona,
            boolean isDomainIdentified
    ) {}

    // Définition d'un domaine sémantique avec ses mots-clés et son persona dédié
    private record DomainDefinition(
            String name,
            List<String> keywords,
            String personaTemplate
    ) {}

    private static final List<DomainDefinition> DOMAIN_REGISTRY = List.of(
            // 1. Musique, Audio & Lutherie
            new DomainDefinition(
                    "Musique & Lutherie",
                    List.of("guitare", "piano", "batterie", "violon", "violoncelle", "basse", "trompette", "saxophone", "flute", "solfege", "accord", "gamme", "synthe", "harmonie", "luthier", "lutherie", "partition", "tempo", "mixage", "mastering", "chant", "composition", "ampli", "archet", "instrument"),
                    "Tu es un Maître Musicien, Compositeur et Pédagogue Musical de renommée internationale."
            ),

            // 2. Gastronomie & Arts Culinaires
            new DomainDefinition(
                    "Gastronomie & Arts Culinaires",
                    List.of("recette", "cuisine", "cuisiner", "cuisson", "pate", "gateau", "tarte", "four", "sauce", "patisserie", "chef", "ingredient", "gastronomie", "epice", "confiture", "chocolat", "vin", "sommelier", "pain", "levain"),
                    "Tu es un Chef Étoilé et Expert Pédagogue en Haute Gastronomie."
            ),

            // 3. Aéronautique, Espace & Transports
            new DomainDefinition(
                    "Aéronautique & Systèmes de Transport",
                    List.of("avion", "vol", "voler", "cockpit", "pilotage", "pilote", "atterrissage", "decollage", "portance", "reacteur", "train", "locomotive", "ferroviaire", "tgv", "rail", "fusee", "satellite", "orbite", "propulsion", "aerodynamique"),
                    "Tu es un Ingénieur Senior en Aéronautique & Systèmes de Transport et Pilote Instructeur."
            ),

            // 4. Astrophysique, Cosmologie & Physique Fondamentale
            new DomainDefinition(
                    "Astrophysique & Sciences Fondamentales",
                    List.of("astrophysique", "trou noir", "trous noirs", "gravitation", "relativite", "atome", "quantique", "telescope", "galaxie", "energie", "vitesse lumiere", "photon", "boson", "cosmos", "etoile", "planete", "matiere noire", "big bang"),
                    "Tu es un Astrophysicien et Enseignant-Chercheur Émérite en Cosmologie."
            ),

            // 5. Génie Mécanique & Ingénierie Industrielle
            new DomainDefinition(
                    "Génie Mécanique & Industrie",
                    List.of("moteur", "mecanique", "engrenage", "piston", "usinage", "hydraulique", "soudure", "machine", "transmission", "couple", "dynamo", "robotique", "electromecanique", "thermodynamique", "chassis"),
                    "Tu es un Ingénieur Mécanicien Senior et Concepteur Industriel de référence."
            ),

            // 6. Sport, Biomécanique & Préparation Physique
            new DomainDefinition(
                    "Sciences du Sport & Biomécanique",
                    List.of("sport", "musculation", "course", "marathon", "cardio", "golf", "swing", "tennis", "football", "rugby", "natation", "endurance", "etirement", "posture", "biomecanique", "nutrition sportive", "entrainement", "muscles"),
                    "Tu es un Préparateur Physique d'Athlètes de Haut Niveau et Expert en Biomécanique."
            ),

            // 7. Histoire, Civilisations & Géopolitique
            new DomainDefinition(
                    "Histoire & Sciences des Civilisations",
                    List.of("histoire", "moyen age", "guerre", "revolution", "empire", "antiquite", "siecle", "traite", "monarque", "civilisation", "bataille", "roi", "republique", "geopolitique", "dynastie", "renaissance"),
                    "Tu es un Historien Universitaire et Érudit Spécialiste des Civilisations."
            ),

            // 8. Droit, Législation & Fiscalité
            new DomainDefinition(
                    "Droit & Réglementation",
                    List.of("droit", "juridique", "loi", "contrat", "tribunal", "avocat", "fiscalite", "code civil", "penal", "jurisprudence", "litige", "notaire", "cdi", "cdd", "bail", "statut", "reglementation", "clause"),
                    "Tu es un Juriste Émérite et Conseiller Spécialiste du Droit et de la Réglementation."
            ),

            // 9. Économie & Stratégie Financière
            new DomainDefinition(
                    "Économie & Marchés Financiers",
                    List.of("finance", "bourse", "investissement", "action", "inflation", "pib", "dividende", "obligation", "marche financier", "trading", "macroeconomie", "fiscal", "rendement", "cryptomonnaie", "bitcoin"),
                    "Tu es un Économiste et Analyste Stratégique de Marché de premier plan."
            ),

            // 10. Médecine & Neurosciences
            new DomainDefinition(
                    "Médecine & Sciences Biomédicales",
                    List.of("medecine", "anatomie", "neurone", "cerveau", "symptome", "vaccin", "immunite", "cellule", "adn", "virus", "bacterie", "physiologie", "pathologie", "traitement", "biologie", "cardiologie", "immunologie"),
                    "Tu es un Médecin et Chercheur Spécialiste en Physiologie & Sciences Biomédicales."
            )
    );

    // Modèles d'extraction d'intention et de sujet pivot
    private static final List<Pattern> TOPIC_EXTRACTION_PATTERNS = List.of(
            // Intentions d'apprentissage / maîtrise / explications
            Pattern.compile("(?i)\\b(?:apprendre|comprendre|jouer|joue|pratiquer|connaitre|decouvrir|maitriser|expliquer|explique|exposer|analyser|decrire|detaille|detailler|presente|presenter)\\s+(?:a\\s+|au\\s+|aux\\s+|la\\s+|le\\s+|les\\s+|de\\s+la\\s+|du\\s+|des\\s+|l['\\s]|un\\s+|une\\s+)?([^.?!,;\\n]+)"),

            // Fonctionnement / mécanisme
            Pattern.compile("(?i)\\b(?:comment\\s+marche|comment\\s+fonctionne|fonctionnement\\s+(?:de\\s+|d['\\s]|du\\s+|des\\s+|de\\s+la\\s+)|mecanisme\\s+(?:de\\s+|d['\\s]|du\\s+|des\\s+))([^.?!,;\\n]+)"),

            // Recette / préparation culinaire
            Pattern.compile("(?i)\\b(?:recette(?:\\s+et\\s+les\\s+etapes)?|cuisin(?:er|e|ez)?|preparer|faire|concocter)\\s+(?:de\\s+|d['\\s]|du\\s+|des\\s+|de\\s+la\\s+|un\\s+|une\\s+|pour\\s+faire\\s+|pour\\s+cuisiner\\s+|pour\\s+preparer\\s+)?([^.?!,;\\n]+)"),

            // Histoire / origines
            Pattern.compile("(?i)\\b(?:histoire|origine|evolution|biographie)\\s+(?:de\\s+|d['\\s]|du\\s+|des\\s+|de\\s+la\\s+)?([^.?!,;\\n]+)"),

            // Définition / vulgarisation
            Pattern.compile("(?i)\\b(?:qu['\\s]*est[\\s-]*ce\\s+qu[e']|c['\\s]*est\\s+quoi|definition\\s+(?:de\\s+|d['\\s]|du\\s+|des\\s+)|qu['\\s]*est[\\s-]*ce\\s+(?:qu['\\s]|le\\s+|la\\s+|l['\\s]|un\\s+|une\\s+|du\\s+))([^.?!,;\\n]+)"),

            // Guides et techniques
            Pattern.compile("(?i)\\b(?:regle\\s+(?:de\\s+|du\\s+|des\\s+|d['\\s])|technique\\s+(?:de\\s+|du\\s+|des\\s+|d['\\s])|exercice\\s+(?:de\\s+|du\\s+|des\\s+|d['\\s])|guide\\s+(?:de\\s+|du\\s+|des\\s+|d['\\s]|pour\\s+))([^.?!,;\\n]+)")
    );

    // Analyse le texte brut pour extraire le domaine, le sujet pivot et le persona
    public static DomainInfo analyser(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new DomainInfo("Général", "", "Tu es un Assistant IA Expert de haut niveau.", false);
        }

        String texteNettoye = Sanitzer.restaurerElisions(rawText.trim());
        String lower = " " + texteNettoye.toLowerCase() + " ";

        // 1. Extraction du sujet pivot via les patterns syntaxiques
        String extractedTopic = extraireSujetPivot(texteNettoye);

        // 2. Recherche du domaine sémantique correspondant
        DomainDefinition matchingDomain = null;
        int maxScore = 0;

        for (DomainDefinition def : DOMAIN_REGISTRY) {
            int score = 0;
            for (String kw : def.keywords()) {
                String regex = "(?i)(?<![a-zA-Z0-9])" + Pattern.quote(kw) + "(?![a-zA-Z0-9])";
                if (Pattern.compile(regex).matcher(lower).find()) {
                    score += 2;
                }
            }

            if (score > maxScore) {
                maxScore = score;
                matchingDomain = def;
            }
        }

        // 3. Forger le persona
        if (matchingDomain != null && maxScore >= 2) {
            return new DomainInfo(
                    matchingDomain.name(),
                    extractedTopic.isBlank() ? matchingDomain.name() : extractedTopic,
                    matchingDomain.personaTemplate(),
                    true
            );
        }

        // Si aucun domaine spécifique n'a été trouvé, forger un persona dynamique basé sur le sujet extrait
        if (!extractedTopic.isBlank() && extractedTopic.length() >= 3) {
            String personaDynamique = "Tu es un Spécialiste de référence et Pédagogue d'élite en " + extractedTopic + ", reconnu pour ton expertise et ta clarté.";
            return new DomainInfo("Expertise Thématique", extractedTopic, personaDynamique, true);
        }

        // Fallback universel
        return new DomainInfo("Général", "", "Tu es un Assistant IA Expert et Pédagogue de haut niveau.", false);
    }

    // Crée un profil de domaine forcé par l'utilisateur (flag -d/--domain)
    public static DomainInfo creerDomaineForce(String nomDomaine, String sujet) {
        if (nomDomaine == null || nomDomaine.isBlank()) {
            return new DomainInfo("Général", sujet != null ? sujet : "", "Tu es un Assistant IA Expert et Pédagogue de haut niveau.", false);
        }

        String dLower = nomDomaine.toLowerCase().trim();
        for (DomainDefinition def : DOMAIN_REGISTRY) {
            if (def.name().toLowerCase().contains(dLower) || dLower.contains(def.name().toLowerCase())) {
                return new DomainInfo(def.name(), (sujet != null && !sujet.isBlank()) ? sujet : def.name(), def.personaTemplate(), true);
            }
        }

        String personaCustom = "Tu es un Spécialiste Senior, Consultant et Pédagogue d'Élite en " + nomDomaine.trim() + ".";
        return new DomainInfo(nomDomaine.trim(), (sujet != null && !sujet.isBlank()) ? sujet : nomDomaine.trim(), personaCustom, true);
    }

    // Extrait le groupe nominal principal après le verbe d'action
    private static String extraireSujetPivot(String text) {
        for (Pattern pattern : TOPIC_EXTRACTION_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String sujet = matcher.group(1).trim();
                sujet = nettoyerBruitSujet(sujet);
                if (!sujet.isBlank()) {
                    return sujet;
                }
            }
        }

        // Si pas de pattern verbe, mais un prompt court de 1 à 4 mots (ex: "guitare acoustique", "locomotive a vapeur")
        String[] mots = text.split("\\s+");
        if (mots.length <= 4) {
            return nettoyerBruitSujet(text.trim());
        }

        return "";
    }

    // Nettoie les formules de politesse, verbes parasites ou connecteurs
    private static String nettoyerBruitSujet(String sujet) {
        if (sujet == null) return "";

        String clean = sujet.trim();
        clean = clean.replaceAll("(?i)^(?:et\\s+les\\s+etapes\\s+(?:pour\\s+(?:faire|preparer|cuisiner))?|pour\\s+(?:faire|preparer|cuisiner)|les\\s+etapes\\s+pour|comment\\s+faire)[,\\s]+", "");
        clean = clean.replaceAll("(?i)[,\\s]+(merci|stp|s il te plait|s'il te plaît|s'il vous plaît|svp|merci beaucoup|rapidement|facilement|simplement)[.!?\\s]*$", "");
        clean = clean.replaceAll("(?i)[,\\s]+(et comment|et pourquoi|et apres|et ensuite).*$", "");
        clean = clean.replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "");

        return clean.trim();
    }
}
