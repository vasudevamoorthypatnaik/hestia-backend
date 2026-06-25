package com.hestia.event.domain;

import java.util.UUID;

/** A household — the unit a calendar belongs to. */
public record Household(UUID id, String name, String timezone) {}
