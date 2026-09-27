package service;

import java.util.*;

/**
 * Rule-based priority scoring engine.
 * Scores a complaint based on urgency keywords, category risk, and (optionally)
 * how many similar complaints were recently filed.
 *
 * This is the "unique" part of the project - a simple, explainable scoring
 * algorithm instead of a generic CRUD form.
 */
public class PriorityScorer {

    // Words that signal a genuinely urgent / unsafe situation
    private static final List<String> HIGH_URGENCY_WORDS = Arrays.asList(
            "fire", "shock", "electric shock", "harassment", "ragging",
            "food poisoning", "gas leak", "leak", "unsafe", "injury", "assault"
    );

    // Words that signal an annoyance but not a safety issue
    private static final List<String> MILD_URGENCY_WORDS = Arrays.asList(
            "dirty", "noisy", "broken", "delay", "late", "cold food", "smell"
    );

    // Categories that are inherently higher risk
    private static final Set<String> HIGH_RISK_CATEGORIES = new HashSet<>(Arrays.asList(
            "electrical", "water", "harassment", "fire", "safety"
    ));

    /**
     * Computes a 0-100+ priority score for a complaint.
     *
     * @param category         complaint category (lowercase)
     * @param description      free text description
     * @param recentSimilarCount how many similar complaints (same category, overlapping
     *                           keywords) were filed by others in the last 7 days
     */
    public static int computeScore(String category, String description, int recentSimilarCount) {
        int score = 0;
        String lowerDesc = description == null ? "" : description.toLowerCase();
        String lowerCat = category == null ? "" : category.toLowerCase();

        // 1. Keyword urgency scan
        for (String word : HIGH_URGENCY_WORDS) {
            if (lowerDesc.contains(word)) {
                score += 30;
                break; // only count once, don't stack multiple urgent words
            }
        }
        for (String word : MILD_URGENCY_WORDS) {
            if (lowerDesc.contains(word)) {
                score += 10;
                break;
            }
        }

        // 2. Category risk weight
        if (HIGH_RISK_CATEGORIES.contains(lowerCat)) {
            score += 20;
        } else {
            score += 5;
        }

        // 3. Repeat complaint bonus - signals a real, ongoing unresolved issue
        if (recentSimilarCount >= 3) {
            score += 15;
        }

        return score;
    }

    /** Converts a numeric score into a human-readable tier. */
    public static String computeTier(int score) {
        if (score >= 50) return "HIGH";
        if (score >= 25) return "MEDIUM";
        return "LOW";
    }

    /**
     * Returns the deadline window (in hours) based on priority tier.
     * HIGH = 24h, MEDIUM = 72h (3 days), LOW = 168h (7 days)
     */
    public static int deadlineHoursForTier(String tier) {
        switch (tier) {
            case "HIGH": return 24;
            case "MEDIUM": return 72;
            default: return 168;
        }
    }

    /** Simple keyword overlap check used to detect "similar" complaints. */
    public static boolean isSimilar(String desc1, String desc2) {
        Set<String> words1 = new HashSet<>(Arrays.asList(desc1.toLowerCase().split("\\W+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(desc2.toLowerCase().split("\\W+")));
        words1.retainAll(words2); // intersection
        // ignore very short/common words
        words1.removeIf(w -> w.length() <= 3);
        return words1.size() >= 2; // 2+ shared meaningful keywords = similar
    }
}
