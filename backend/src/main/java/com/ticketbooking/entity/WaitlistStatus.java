package com.ticketbooking.entity;

public enum WaitlistStatus {
    WAITING,     // still in the queue
    PROMOTED,    // a seat opened up and a booking was created automatically
    CANCELLED    // user left the queue
}
