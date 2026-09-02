package com.ecommerce.modules.address.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.address.entity.UserAddress;
import com.ecommerce.modules.address.mapper.AddressMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/address")
public class AddressController {

    @Resource
    private AddressMapper addressMapper;

    @GetMapping("/list")
    public Result getAddressList(@RequestHeader(value = "token", required = false) String token) {
        // 从token中获取用户ID，这里暂时从前端传递用户ID
        // 实际项目中应该解析token获取用户ID
        Long userId = getUserIdFromToken(token);

        List<UserAddress> addresses = addressMapper.selectByUserId(userId);
        return Result.success(addresses);
    }

    @PostMapping("/add")
    public Result addAddress(@RequestBody UserAddress address, @RequestHeader(value = "token", required = false) String token) {
        // 从token中获取用户ID，这里暂时从前端传递用户ID
        // 实际项目中应该解析token获取用户ID
        Long userId = getUserIdFromToken(token);
        address.setUserId(userId);

        // 如果设置为默认地址，先将其他地址设置为非默认
        if (address.getIsDefault() == 1) {
            addressMapper.updateDefaultToFalse(userId);
        }

        int result = addressMapper.insert(address);
        if (result > 0) {
            return Result.success("添加地址成功");
        } else {
            return Result.fail("添加地址失败");
        }
    }

    @PutMapping("/update")
    public Result updateAddress(@RequestBody UserAddress address, @RequestHeader(value = "token", required = false) String token) {
        // 从token中获取用户ID，这里暂时从前端传递用户ID
        // 实际项目中应该解析token获取用户ID
        Long userId = getUserIdFromToken(token);
        address.setUserId(userId);

        // 如果设置为默认地址，先将其他地址设置为非默认
        if (address.getIsDefault() == 1) {
            addressMapper.updateDefaultToFalse(userId);
        }

        int result = addressMapper.update(address);
        if (result > 0) {
            return Result.success("更新地址成功");
        } else {
            return Result.fail("更新地址失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result deleteAddress(@PathVariable Long id, @RequestHeader(value = "token", required = false) String token) {
        // 从token中获取用户ID，这里暂时从前端传递用户ID
        // 实际项目中应该解析token获取用户ID
        Long userId = getUserIdFromToken(token);

        UserAddress address = addressMapper.selectById(id);
        if (address != null && address.getUserId().equals(userId)) {
            int result = addressMapper.delete(id);
            if (result > 0) {
                return Result.success("删除地址成功");
            } else {
                return Result.fail("删除地址失败");
            }
        } else {
            return Result.fail("地址不存在");
        }
    }

    @PutMapping("/default/{id}")
    public Result setDefaultAddress(@PathVariable Long id, @RequestHeader(value = "token", required = false) String token) {
        // 从token中获取用户ID，这里暂时从前端传递用户ID
        // 实际项目中应该解析token获取用户ID
        Long userId = getUserIdFromToken(token);

        UserAddress address = addressMapper.selectById(id);
        if (address != null && address.getUserId().equals(userId)) {
            // 将其他地址设置为非默认
            addressMapper.updateDefaultToFalse(userId);
            // 将当前地址设置为默认
            address.setIsDefault(1);
            int result = addressMapper.update(address);
            if (result > 0) {
                return Result.success("设置默认地址成功");
            } else {
                return Result.fail("设置默认地址失败");
            }
        } else {
            return Result.fail("地址不存在");
        }
    }

    // 从token中获取用户ID，这里暂时从token中提取用户ID
    // 实际项目中应该使用JWT解析token获取用户ID
    private Long getUserIdFromToken(String token) {
        // 这里暂时返回固定值1，实际项目中应该解析token
        // 例如：JWT解析token获取用户ID
        // 为了测试，我们可以从token中提取用户ID
        if (token != null && token.contains("_")) {
            try {
                String[] parts = token.split("_");
                return Long.parseLong(parts[1]);
            } catch (Exception e) {
                // 如果解析失败，返回默认值1
                return 1L;
            }
        }
        return 1L;
    }
}
