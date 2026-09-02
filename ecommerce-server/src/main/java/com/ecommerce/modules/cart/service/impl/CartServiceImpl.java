package com.ecommerce.modules.cart.service.impl;

import com.ecommerce.modules.cart.entity.Cart;
import com.ecommerce.modules.cart.mapper.CartMapper;
import com.ecommerce.modules.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartMapper cartMapper;

    @Override
    public List<Cart> getCartList(Long userId) {
        return cartMapper.selectByUserId(userId);
    }

    @Override
    public void addCart(Long userId, Long productId, Integer quantity) {
        // 检查数量是否小于1
        if (quantity < 1) {
            throw new IllegalArgumentException("数量不能小于1");
        }

        // 检查是否已存在该商品
        Cart existingCart = cartMapper.selectByUserIdAndProductId(userId, productId);
        if (existingCart != null) {
            // 存在则数量叠加
            int newQuantity = existingCart.getQuantity() + quantity;
            cartMapper.updateQuantity(existingCart.getId(), newQuantity);
        } else {
            // 不存在则新增
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setQuantity(quantity);
            cart.setSelected(1); // 默认选中
            cart.setCreateTime(new Date());
            cart.setUpdateTime(new Date());
            cartMapper.insert(cart);
        }
    }

    @Override
    public void updateQuantity(Long id, Long userId, Integer quantity) {
        // 检查数量是否小于1
        if (quantity < 1) {
            throw new IllegalArgumentException("数量不能小于1");
        }
        // 验证购物车项是否属于当前用户
        Cart cart = cartMapper.selectById(id);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new IllegalArgumentException("购物车项不存在或无权限操作");
        }
        cartMapper.updateQuantity(id, quantity);
    }

    @Override
    public void updateSelected(Long id, Long userId, Integer selected) {
        // 验证购物车项是否属于当前用户
        Cart cart = cartMapper.selectById(id);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new IllegalArgumentException("购物车项不存在或无权限操作");
        }
        cartMapper.updateSelected(id, selected);
    }

    @Override
    public void deleteCart(Long id, Long userId) {
        // 验证购物车项是否属于当前用户
        Cart cart = cartMapper.selectById(id);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new IllegalArgumentException("购物车项不存在或无权限操作");
        }
        cartMapper.deleteById(id);
    }

    @Override
    public void clearSelectedCart(Long userId) {
        cartMapper.deleteSelectedByUserId(userId);
    }
}
