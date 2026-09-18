package com.hellfire.repository;

import com.hellfire.model.User;
import com.hellfire.model.UserRole;
import com.hellfire.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    User findByEmail(String username);

    long countByRole(UserRole role);

    long countByRoleAndStatus(UserRole role, UserStatus status);

    long countByRoleAndCreatedAtAfter(UserRole role, LocalDateTime after);

    List<User> findTop5ByRoleOrderByCreatedAtDesc(UserRole role);

    List<User> findByRoleInOrderByCreatedAtDesc(Collection<UserRole> roles);

    /** Legacy rows created before the status column existed. */
    @Modifying
    @Query("update User u set u.status = :status where u.status is null")
    int backfillMissingStatus(@Param("status") UserStatus status);

    @Modifying
    @Query("update User u set u.createdAt = :now where u.createdAt is null")
    int backfillMissingCreatedAt(@Param("now") LocalDateTime now);
}
