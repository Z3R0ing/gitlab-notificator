package ru.z3r0ing.gitlabnotificator.model.gitlab.object;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    private Long id;

    private String name;

    private String username;
}
