package com.qscout.spring.web.dto;

public final class ScoreBandClassResolver {
    public static final String HIGH = "score-band-high";
    public static final String MEDIUM = "score-band-medium";
    public static final String LOW = "score-band-low";

    private ScoreBandClassResolver() {
    }

    public static String fromScore(int score) {
        if (score >= 80) {
            return HIGH;
        }
        if (score >= 40) {
            return MEDIUM;
        }
        return LOW;
    }
}
