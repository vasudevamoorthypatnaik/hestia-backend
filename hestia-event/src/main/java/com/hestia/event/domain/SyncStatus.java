package com.hestia.event.domain;

/** Connected-account sync state (seed-only in this slice — no live integration). */
public enum SyncStatus {
    SYNCED,
    DISCONNECTED
}
