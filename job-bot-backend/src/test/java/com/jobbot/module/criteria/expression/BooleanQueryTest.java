package com.jobbot.module.criteria.expression;

import com.jobbot.common.exception.JobBotException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BooleanQueryTest {

    @Test
    void simpleAndRequiresAllTerms() {
        BooleanQuery q = BooleanQuery.parse("Java AND Spring Boot");
        assertTrue(q.matches("We use Java and Spring Boot in production"));
        assertFalse(q.matches("We use Java and Node"));
    }

    @Test
    void orMatchesEither() {
        BooleanQuery q = BooleanQuery.parse("Kafka OR RabbitMQ");
        assertTrue(q.matches("event streaming with Kafka"));
        assertTrue(q.matches("messaging via RabbitMQ"));
        assertFalse(q.matches("plain REST service"));
    }

    @Test
    void notExcludes() {
        BooleanQuery q = BooleanQuery.parse("Java AND NOT Intern");
        assertTrue(q.matches("Senior Java Developer"));
        assertFalse(q.matches("Java Intern position"));
    }

    @Test
    void parenthesesGroupPrecedence() {
        BooleanQuery q = BooleanQuery.parse("Java AND (Kafka OR Microservices) AND AWS");
        assertTrue(q.matches("Java Microservices on AWS"));
        assertTrue(q.matches("Java with Kafka and AWS"));
        assertFalse(q.matches("Java and AWS only")); // missing Kafka/Microservices
    }

    @Test
    void multiWordPhraseMatchesAsPhrase() {
        BooleanQuery q = BooleanQuery.parse("Spring Boot");
        assertTrue(q.matches("built with Spring Boot"));
        assertFalse(q.matches("Spring Framework and Boot camp")); // not the phrase
    }

    @Test
    void wordBoundaryPreventsPartialMatches() {
        BooleanQuery q = BooleanQuery.parse("Java");
        assertTrue(q.matches("core Java engineer"));
        assertFalse(q.matches("JavaScript developer")); // 'java' inside 'javascript' must not match
    }

    @Test
    void evaluatesAgainstSkillList() {
        BooleanQuery q = BooleanQuery.parse("Java AND (Kafka OR Microservices)");
        assertTrue(q.matchesSkills(List.of("Java", "Kafka", "AWS")));
        assertFalse(q.matchesSkills(List.of("Java", "AWS")));
    }

    @Test
    void caseInsensitiveOperatorsAndTerms() {
        BooleanQuery q = BooleanQuery.parse("java and spring boot");
        assertTrue(q.matches("JAVA and SPRING BOOT"));
    }

    @Test
    void invalidExpressionThrows() {
        assertThrows(JobBotException.class, () -> BooleanQuery.parse("Java AND"));
        assertThrows(JobBotException.class, () -> BooleanQuery.parse("(Java AND Kafka"));
        assertThrows(JobBotException.class, () -> BooleanQuery.parse("   "));
    }
}

