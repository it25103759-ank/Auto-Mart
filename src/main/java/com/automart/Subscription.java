package com.automart;

final class Subscription {
    private final String planName;
    private final Payment payment;
    private boolean active;

    Subscription(String planName, Payment payment) {
        this.planName = planName;
        this.payment = payment;
        this.active = payment != null && payment.isValid();
    }

    String getPlanName() { return planName; }
    boolean isActive() { return active; }
    Payment getPayment() { return payment; }
    void cancel() { active = false; }
}
