package rip.kill9.terminator.config.ex20;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

public class AttackModels {

    @Data
    public static class AttackLog {
        private long id;
        private LocalDateTime timestamp;
        private String targetIp;
        private AttackType attackType;
        private String payload;
        private SeverityLevel severityLevel;  // 추가
    }

    enum SeverityLevel {
        CRITICAL, // 치명적 공격 (즉시 대응 필요)
        NORMAL;   // 일반 공격 (로그 기록)

        @JsonCreator
        public static SeverityLevel fromString(String value) {
            if (value == null) return NORMAL;

            return switch (value.toUpperCase()) {
                case "CRITICAL" -> CRITICAL;
                case "NORMAL" -> NORMAL;
                default -> NORMAL;
            };
        }
    }

    public enum AttackType {
        SQL_INJECTION("SQL Injection"),
        XSS("Cross-Site Scripting (XSS)"),
        DDOS("DDoS"),
        BRUTE_FORCE("Brute Force"),
        UNKNOWN("Unknown");

        private final String displayName;

        AttackType(String displayName) {
            this.displayName = displayName;
        }

        @JsonCreator
        public static AttackType fromString(String attackType) {
            if (attackType == null) return UNKNOWN;

            return switch (attackType.toLowerCase()) {
                case "sql injection" -> SQL_INJECTION;
                case "cross-site scripting (xss)", "xss" -> XSS;
                case "ddos" -> DDOS;
                case "brute force" -> BRUTE_FORCE;
                default -> UNKNOWN;
            };
        }
    }

    @Builder
    @Getter
    public static class AttackAnalysisResult {
        private static final JsonMapper PRETTY_MAPPER =
            JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

        private int totalAttacks;
        private Map<AttackType, Integer> attackTypeCount;
        private Map<AttackType, String> attackTypePercentage;
        private Map<String, Integer> ipAttackCount;
        private Map<String, Integer> timeSlotCount;
        private String mostDangerousIp;
        private String peakHour;
        private String threatLevel;

        @Override
        public String toString() {
            try {
                return """
               💀 ========== KILL-9 공격 분석 결과 ==========
               %s
               💀 ============================================
               """.formatted(PRETTY_MAPPER.writeValueAsString(this));
            } catch (Exception e) {
                return super.toString();
            }
        }
    }
}