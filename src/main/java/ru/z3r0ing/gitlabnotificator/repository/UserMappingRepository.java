package ru.z3r0ing.gitlabnotificator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.z3r0ing.gitlabnotificator.model.UserRole;
import ru.z3r0ing.gitlabnotificator.model.entity.UserMapping;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserMappingRepository extends JpaRepository<UserMapping, Long> {
    Optional<UserMapping> findByTelegramId(Long telegramId);
    Optional<UserMapping> findByGitlabUserId(Long gitlabUserId);
    List<UserMapping> findAllByRole(UserRole role);
}
