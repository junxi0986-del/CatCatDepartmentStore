package com.ecommerce.modules.cart.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.cart.entity.Cart;
import com.ecommerce.modules.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    // 获取购物车列表
    @GetMapping("/list")
    public Result getCartList(@RequestHeader(value = "token", required = false) String token) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return Result.fail("请先登录");
        }
        List<Cart> cartList = cartService.getCartList(userId);
        return Result.success(cartList);
    }

    // 添加购物车
    @PostMapping("/add")
    public Result addCart(@RequestHeader(value = "token", required = false) String token, @RequestParam Long productId, @RequestParam Integer quantity) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return Result.fail("请先登录");
        }
        cartService.addCart(userId, productId, quantity);
        return Result.success("添加购物车成功");
    }

    // 修改购物车数量
    @PutMapping("/update/quantity")
    public Result updateQuantity(@RequestHeader(value = "token", required = false) String token, @RequestParam Long id, @RequestParam Integer quantity) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return Result.fail("请先登录");
        }
        try {
            cartService.updateQuantity(id, userId, quantity);
            return Result.success("修改数量成功");
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    // 修改购物车选中状态
    @PutMapping("/update/selected")
    public Result updateSelected(@RequestHeader(value = "token", required = false) String token, @RequestParam Long id, @RequestParam Integer selected) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return Result.fail("请先登录");
        }
        try {
            cartService.updateSelected(id, userId, selected);
            return Result.success("修改选中状态成功");
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    // 删除购物车
    @DeleteMapping("/delete")
    public Result deleteCart(@RequestHeader(value = "token", required = false) String token, @RequestParam Long id) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return Result.fail("请先登录");
        }
        try {
            cartService.deleteCart(id, userId);
            return Result.success("删除购物车成功");
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    // 清空已选中购物车
    @DeleteMapping("/clear")
    public Result clearSelectedCart(@RequestHeader(value = "token", required = false) String token) {
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return Result.fail("请先登录");
        }
        cartService.clearSelectedCart(userId);
        return Result.success("清空已选中购物车成功");
    }

    // 从token中获取用户ID
    private Long getUserIdFromToken(String token) {
        if (token != null && token.contains("_")) {
            try {
                String[] parts = token.split("_");
                return Long.parseLong(parts[1]);
            } catch (Exception e) {
                // 如果解析失败，返回默认值1
                return 1L;
            }
        }
        // 如果token为空或格式不正确，返回默认值1
        return 1L;
    }
}
