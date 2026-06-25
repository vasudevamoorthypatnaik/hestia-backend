package com.hestia.user.application;

import java.util.Optional;

/** Query port for reading a user's profile (the `me` query). */
public interface UserQueryService {

    record MeView(
            String id, String email, String firstName, String lastName, String preferredLanguage) {}

    Optional<MeView> findById(String userId);
}
