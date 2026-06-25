package com.hestia.event.domain;

/** Household role. NONE is used for children (not app users, no permissions). */
public enum MemberRole {
    ADMIN,
    MEMBER,
    NONE
}
