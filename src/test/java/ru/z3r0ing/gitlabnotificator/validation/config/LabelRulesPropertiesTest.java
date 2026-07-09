package ru.z3r0ing.gitlabnotificator.validation.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LabelRulesPropertiesTest {

    @Configuration
    // Not @ConfigurationPropertiesScan: it deliberately skips @Component-annotated classes
    // (ConfigurationPropertiesScanRegistrar.register), and LabelRulesProperties is a @Component
    // so that production picks it up via component scan, like config/AppProperties.
    @EnableConfigurationProperties(LabelRulesProperties.class)
    static class TestConfig {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(UserConfigurations.of(TestConfig.class));

    @Test
    void binding_ValidConfig_ShouldBindAllFields() {
        runner.withPropertyValues(
                "label-rules.enabled=true",
                "label-rules.debounce=5m",
                "label-rules.escalation-delay=1h",
                "label-rules.forbid-unknown-labels=true",
                "label-rules.groups[0].name=Status",
                "label-rules.groups[0].prefix=S:",
                "label-rules.groups[0].min=1",
                "label-rules.groups[0].max=1"
        ).run(context -> {
            LabelRulesProperties properties = context.getBean(LabelRulesProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getDebounce()).isEqualTo(Duration.ofMinutes(5));
            assertThat(properties.getEscalationDelay()).isEqualTo(Duration.ofHours(1));
            assertThat(properties.isForbidUnknownLabels()).isTrue();
            assertThat(properties.getGroups()).hasSize(1);
            assertThat(properties.getGroups().get(0).getPrefix()).isEqualTo("S:");
            assertThat(properties.getGroups().get(0).getMax()).isEqualTo(1);
        });
    }

    @Test
    void binding_MaxOmitted_ShouldDefaultToNullAndMinToZero() {
        runner.withPropertyValues(
                "label-rules.groups[0].name=Environment",
                "label-rules.groups[0].prefix=E:"
        ).run(context -> {
            LabelRulesProperties.Group group =
                    context.getBean(LabelRulesProperties.class).getGroups().get(0);
            assertThat(group.getMin()).isZero();
            assertThat(group.getMax()).isNull();
        });
    }

    @Test
    void binding_MinGreaterThanMax_ShouldFailStartup() {
        runner.withPropertyValues(
                "label-rules.groups[0].name=Status",
                "label-rules.groups[0].prefix=S:",
                "label-rules.groups[0].min=2",
                "label-rules.groups[0].max=1"
        ).run(context -> assertThat(context).hasFailed());
    }

    @Test
    void binding_DuplicatePrefixes_ShouldFailStartup() {
        runner.withPropertyValues(
                "label-rules.groups[0].name=Status",
                "label-rules.groups[0].prefix=S:",
                "label-rules.groups[1].name=Source",
                "label-rules.groups[1].prefix=S:"
        ).run(context -> assertThat(context).hasFailed());
    }

    @Test
    void binding_NonPositiveDebounce_ShouldFailStartup() {
        runner.withPropertyValues(
                "label-rules.debounce=0s"
        ).run(context -> assertThat(context).hasFailed());
    }

    @Test
    void binding_BlankPrefix_ShouldFailStartup() {
        runner.withPropertyValues(
                "label-rules.groups[0].name=Status",
                "label-rules.groups[0].prefix="
        ).run(context -> assertThat(context).hasFailed());
    }
}
