package com.Ecommerce.Cart.Service.ScheduledTaskes;



import com.Ecommerce.Cart.Service.Services.ShoppingCartService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CartCleanupTask {
    private final ShoppingCartService cartService;

    @Scheduled(cron = "0 0 0 * * ?") // Run at midnight every day
    public void cleanupExpiredCarts() {
        cartService.cleanupExpiredCarts();
    }
}