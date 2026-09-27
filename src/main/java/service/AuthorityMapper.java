package service;

/**
 * Category → designation at each escalation level (0 = initial, 1 = escalated, 2 = final).
 */
public final class AuthorityMapper {

    private static final String[] CATEGORIES = {
            "mess", "hostel", "faculty", "electrical", "water", "harassment", "other"
    };

    private static final String[][] DESIGNATIONS = {
            // mess
            { "Mess Committee", "Hostel Warden", "Principal" },
            // hostel
            { "Hostel Warden", "Dean of Student Affairs", "Principal" },
            // faculty
            { "Department HOD", "Dean of Academics", "Principal" },
            // electrical
            { "Maintenance/Estate Office", "Estate Officer", "Principal" },
            // water
            { "Estate Office", "Estate Officer", "Principal" },
            // harassment
            { "Anti-Ragging Committee", "Dean of Student Affairs", "Principal" },
            // other
            { "Department Office", "Warden/HOD", "Principal" }
    };

    private AuthorityMapper() {}

    public static int categoryIndex(String category) {
        if (category == null) return CATEGORIES.length - 1;
        String c = category.toLowerCase();
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(c)) return i;
        }
        return CATEGORIES.length - 1;
    }

    public static String getDesignation(String category, int level) {
        int idx = categoryIndex(category);
        int lv = Math.max(0, Math.min(level, 2));
        return DESIGNATIONS[idx][lv];
    }

    public static String categoryLabel(String category) {
        return switch (categoryIndex(category)) {
            case 0 -> "Mess / Food";
            case 1 -> "Hostel";
            case 2 -> "Faculty";
            case 3 -> "Electrical";
            case 4 -> "Water";
            case 5 -> "Harassment / Ragging";
            default -> "Other";
        };
    }
}
