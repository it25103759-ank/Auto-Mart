package com.automart;

final class Notification {
    private final String id;
    private final String user;
    private final String title;
    private final String message;
    private boolean read;

    Notification(String id, String user, String title, String message) {
        this.id = id;
        this.user = user;
        this.title = title;
        this.message = message;
    }

    String getId() { return id; }
    String getUser() { return user; }
    String getTitle() { return title; }
    String getMessage() { return message; }
    boolean isRead() { return read; }
    void markRead() { read = true; }
}
