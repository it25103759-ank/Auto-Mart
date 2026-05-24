package com.automart;

final class Listing {
    private final Vehicle vehicle;
    private Status approvalStatus;

    Listing(Vehicle vehicle) {
        this.vehicle = vehicle;
        this.approvalStatus = vehicle == null ? Status.PENDING : vehicle.status;
    }

    Vehicle getVehicle() { return vehicle; }
    Status getApprovalStatus() { return approvalStatus; }
    void approve() { approvalStatus = Status.AVAILABLE; }
    void reject() { approvalStatus = Status.PENDING; }
}
