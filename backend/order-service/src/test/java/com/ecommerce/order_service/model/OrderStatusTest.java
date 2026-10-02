// package com.ecommerce.order_service.model;

// import org.junit.jupiter.api.Test;

// import static org.junit.jupiter.api.Assertions.assertFalse;
// import static org.junit.jupiter.api.Assertions.assertTrue;

// class OrderStatusTest {

//     @Test
//     void pendingCanBeConfirmedOrCancelledOrFailed() {
//         assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED));
//         assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED));
//         assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.FAILED));
//     }

//     @Test
//     void pendingCannotJumpStraightToDelivered() {
//         assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.DELIVERED));
//     }

//     @Test
//     void confirmedCannotGoBackToPending() {
//         assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PENDING));
//     }

//     @Test
//     void cancelledIsTerminal() {
//         assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PENDING));
//         assertFalse(OrderStatus.CANCELLED.isCancelable());
//     }

//     @Test
//     void deliveredIsNotCancelable() {
//         assertFalse(OrderStatus.DELIVERED.isCancelable());
//     }

//     @Test
//     void pendingIsCancelable() {
//         assertTrue(OrderStatus.PENDING.isCancelable());
//     }
// }