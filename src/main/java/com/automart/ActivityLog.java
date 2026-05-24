package com.automart;

final class ActivityLog {
    private final String actor;
    private final String action;
    private final String createdAt;

    ActivityLog(String actor, String action, String createdAt) {
        this.actor = actor;
        this.action = action;
        this.createdAt = createdAt;
    }

    String getActor() { return actor; }
    String getAction() { return action; }
    String getCreatedAt() { return createdAt; }
    String toLine() { return "[" + createdAt + "] " + actor + ": " + action; }
}
