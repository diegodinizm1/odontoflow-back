package com.diego.odontoflowbackend.util;

import java.text.Normalizer;
import java.util.Locale;

/** Turns arbitrary text (e.g. a clinic name) into a URL-safe slug. */
public final class Slugs {

    private Slugs() {}

    public static String slugify(String input) {
        if (input == null) return "clinica";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")              // strip accents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")          // non-alphanumeric -> hyphen
                .replaceAll("(^-+|-+$)", "");           // trim leading/trailing hyphens
        return normalized.isEmpty() ? "clinica" : normalized;
    }
}
