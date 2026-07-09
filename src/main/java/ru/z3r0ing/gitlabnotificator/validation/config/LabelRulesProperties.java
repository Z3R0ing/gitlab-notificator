package ru.z3r0ing.gitlabnotificator.validation.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Declarative label validation rules, loaded from label-rules.yml.
 * Invalid config fails application startup.
 */
@Data
@Component
@ConfigurationProperties(prefix = "label-rules")
@Validated
public class LabelRulesProperties {

    private boolean enabled = true;

    @NotNull
    private Duration debounce = Duration.ofMinutes(5);

    @NotNull
    private Duration escalationDelay = Duration.ofHours(1);

    private boolean forbidUnknownLabels = true;

    @Valid
    private List<Group> groups = new ArrayList<>();

    /**
     * Label group defined by name prefix. Cardinality of active labels
     * is constrained by min/max (max = null means unbounded).
     */
    @Data
    public static class Group {

        @NotBlank
        private String name;

        @NotBlank
        private String prefix;

        @Min(0)
        private int min = 0;

        @Nullable
        @Min(0)
        private Integer max;

        @AssertTrue(message = "group min must be <= max")
        public boolean isCardinalityRangeValid() {
            return max == null || min <= max;
        }
    }

    @AssertTrue(message = "group prefixes must be unique")
    public boolean isPrefixesUnique() {
        long distinct = groups.stream().map(Group::getPrefix).filter(Objects::nonNull).distinct().count();
        return distinct == groups.size();
    }

    @AssertTrue(message = "debounce and escalation-delay must be positive")
    public boolean isTimingsPositive() {
        return debounce != null && !debounce.isZero() && !debounce.isNegative()
                && escalationDelay != null && !escalationDelay.isZero() && !escalationDelay.isNegative();
    }
}
