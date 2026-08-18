package com.jobbot.module.criteria.expression;

import com.jobbot.common.exception.JobBotException;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser for boolean criteria expressions (spec §8/§40).
 *
 * <p>Grammar (precedence NOT &gt; AND &gt; OR):
 * <pre>
 *   or    := and (OR and)*
 *   and   := not (AND not)*
 *   not   := NOT not | atom
 *   atom  := '(' or ')' | term
 *   term  := WORD+            (consecutive words form a phrase, e.g. "Spring Boot")
 * </pre>
 * Operators AND / OR / NOT are case-insensitive.
 */
public final class BooleanQueryParser {

    private final List<Token> tokens;
    private int pos = 0;

    private BooleanQueryParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public static BoolExpr parse(String input) {
        if (input == null || input.isBlank()) {
            throw new JobBotException("Empty boolean expression");
        }
        List<Token> tokens = tokenize(input);
        BooleanQueryParser parser = new BooleanQueryParser(tokens);
        BoolExpr expr = parser.parseOr();
        if (!parser.atEnd()) {
            throw new JobBotException("Unexpected token near: '" + parser.peek().text() + "'");
        }
        return expr;
    }

    // ---- grammar ----

    private BoolExpr parseOr() {
        List<BoolExpr> parts = new ArrayList<>();
        parts.add(parseAnd());
        while (match(TokenType.OR)) {
            parts.add(parseAnd());
        }
        return parts.size() == 1 ? parts.get(0) : new BoolExpr.Or(parts);
    }

    private BoolExpr parseAnd() {
        List<BoolExpr> parts = new ArrayList<>();
        parts.add(parseNot());
        while (match(TokenType.AND)) {
            parts.add(parseNot());
        }
        return parts.size() == 1 ? parts.get(0) : new BoolExpr.And(parts);
    }

    private BoolExpr parseNot() {
        if (match(TokenType.NOT)) {
            return new BoolExpr.Not(parseNot());
        }
        return parseAtom();
    }

    private BoolExpr parseAtom() {
        if (match(TokenType.LPAREN)) {
            BoolExpr inner = parseOr();
            expect(TokenType.RPAREN);
            return inner;
        }
        return parseTerm();
    }

    private BoolExpr parseTerm() {
        StringBuilder sb = new StringBuilder();
        while (!atEnd() && peek().type() == TokenType.WORD) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(advance().text());
        }
        if (sb.length() == 0) {
            String near = atEnd() ? "end of input" : "'" + peek().text() + "'";
            throw new JobBotException("Expected a term but found " + near);
        }
        return new BoolExpr.Term(sb.toString());
    }

    // ---- token helpers ----

    private boolean atEnd() { return pos >= tokens.size(); }
    private Token peek() { return tokens.get(pos); }
    private Token advance() { return tokens.get(pos++); }

    private boolean match(TokenType type) {
        if (!atEnd() && peek().type() == type) { pos++; return true; }
        return false;
    }

    private void expect(TokenType type) {
        if (!match(type)) {
            String near = atEnd() ? "end of input" : "'" + peek().text() + "'";
            throw new JobBotException("Expected " + type + " but found " + near);
        }
    }

    // ---- tokenizer ----

    private enum TokenType { AND, OR, NOT, LPAREN, RPAREN, WORD }
    private record Token(TokenType type, String text) {}

    private static List<Token> tokenize(String input) {
        // Pad parentheses so a simple whitespace split isolates them.
        String padded = input.replace("(", " ( ").replace(")", " ) ");
        String[] raw = padded.trim().split("\\s+");
        List<Token> out = new ArrayList<>();
        for (String r : raw) {
            if (r.isEmpty()) continue;
            switch (r) {
                case "(" -> out.add(new Token(TokenType.LPAREN, r));
                case ")" -> out.add(new Token(TokenType.RPAREN, r));
                default -> {
                    String upper = r.toUpperCase();
                    switch (upper) {
                        case "AND" -> out.add(new Token(TokenType.AND, r));
                        case "OR" -> out.add(new Token(TokenType.OR, r));
                        case "NOT" -> out.add(new Token(TokenType.NOT, r));
                        default -> out.add(new Token(TokenType.WORD, r));
                    }
                }
            }
        }
        if (out.isEmpty()) throw new JobBotException("Empty boolean expression");
        return out;
    }
}

