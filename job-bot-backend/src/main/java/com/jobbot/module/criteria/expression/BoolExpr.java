package com.jobbot.module.criteria.expression;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A boolean criteria expression tree (spec §8/§40): supports AND / OR / NOT over
 * skill/keyword terms, e.g.
 * <pre>Java AND Spring Boot AND (Kafka OR Microservices) AND NOT Intern</pre>
 *
 * Evaluation is deterministic and case-insensitive. A {@link Term} matches when it
 * appears as a whole word (or whole phrase) in the supplied lowercased haystack.
 */
public sealed interface BoolExpr
        permits BoolExpr.Term, BoolExpr.Not, BoolExpr.And, BoolExpr.Or {

    boolean evaluate(String haystackLower);

    record Term(String value) implements BoolExpr {
        @Override
        public boolean evaluate(String haystackLower) {
            if (value == null || value.isBlank() || haystackLower == null) return false;
            Pattern p = Pattern.compile("\\b" + Pattern.quote(value.toLowerCase().trim()) + "\\b");
            return p.matcher(haystackLower).find();
        }
    }

    record Not(BoolExpr inner) implements BoolExpr {
        @Override
        public boolean evaluate(String haystackLower) {
            return !inner.evaluate(haystackLower);
        }
    }

    record And(List<BoolExpr> parts) implements BoolExpr {
        @Override
        public boolean evaluate(String haystackLower) {
            return parts.stream().allMatch(p -> p.evaluate(haystackLower));
        }
    }

    record Or(List<BoolExpr> parts) implements BoolExpr {
        @Override
        public boolean evaluate(String haystackLower) {
            return parts.stream().anyMatch(p -> p.evaluate(haystackLower));
        }
    }
}

