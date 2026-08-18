package com.jobbot.module.criteria.expression;

import java.util.Collection;
import java.util.Locale;

/**
 * A parsed, reusable boolean criteria query (spec §8/§40).
 * Parse once, evaluate many times against job text or a skill list.
 */
public final class BooleanQuery {

    private final String original;
    private final BoolExpr root;

    private BooleanQuery(String original, BoolExpr root) {
        this.original = original;
        this.root = root;
    }

    public static BooleanQuery parse(String expression) {
        return new BooleanQuery(expression, BooleanQueryParser.parse(expression));
    }

    /** Evaluate against a free-text haystack (e.g. a job description). */
    public boolean matches(String text) {
        if (text == null) return false;
        return root.evaluate(text.toLowerCase(Locale.ROOT));
    }

    /** Evaluate against a collection of skills/keywords. */
    public boolean matchesSkills(Collection<String> skills) {
        if (skills == null || skills.isEmpty()) return false;
        String joined = String.join(" | ", skills).toLowerCase(Locale.ROOT);
        return root.evaluate(joined);
    }

    public String getOriginal() {
        return original;
    }

    public BoolExpr getRoot() {
        return root;
    }
}

