package com.automart;

final class Payment {
    private final String method;
    private final String reference;
    private final double amountLkr;

    Payment(String method, String reference, double amountLkr) {
        this.method = method;
        this.reference = reference;
        this.amountLkr = amountLkr;
    }

    String getMethod() { return method; }
    String getReference() { return reference; }
    double getAmountLkr() { return amountLkr; }
    boolean isValid() { return method != null && !method.isBlank() && amountLkr > 0; }
}
