package com.ecommerce.modules.cart.service;

import com.ecommerce.modules.cart.entity.Cart;

import java.util.List;

public interface CartService {
    // 获取购物车列表
    List<Cart> getCartList(Long userId);

    // 添加购物车
    void addCart(Long userId, Long productId, Integer quantity);

    // 修改购物车数量
    void updateQuantity(Long id, Long userId, Integer quantity);

    // 修改购物车选中状态
    void updateSelected(Long id, Long userId, Integer selected);

    // 删除购物车
    void deleteCart(Long id, Long userId);

    // 清空已选中购物车
    void clearSelectedCart(Long userId);
}
