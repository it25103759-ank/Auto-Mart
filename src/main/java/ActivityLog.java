final class ActivityLog {
    private final String actor;
    private final String action;
    private final String createdAt;

    

    String getActor() { return actor; }
    String getAction() { return action; }
    String getCreatedAt() { return createdAt; }
    String toLine() { return "[" + createdAt + "] " + actor + ": " + action; }
}
