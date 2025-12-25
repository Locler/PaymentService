package com.accessChecker;

import com.enums.UserRole;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AccessChecker {

    public void checkAdminAccess(Set<UserRole> roles) {
        if (roles == null || !roles.contains(UserRole.ROLE_ADMIN)) {
            throw new SecurityException("Admin role required");
        }
    }

    public void checkUserAccess(Long targetUserId, Long requesterId, Set<UserRole> roles) {

        if (roles == null || roles.isEmpty()) {
            throw new SecurityException("Roles are missing");
        }

        if (roles.contains(UserRole.ROLE_ADMIN)) {
            return;
        }

        if (roles.contains(UserRole.ROLE_USER) && targetUserId.equals(requesterId)) {
            return;
        }

        throw new SecurityException("Access denied");
    }
}
