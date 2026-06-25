package com.hestia.event.domain;

import java.util.UUID;

/** A connected external calendar account (seed-only in this slice). */
public record ConnectedAccount(
        UUID id, String provider, String label, SyncStatus status) {}
