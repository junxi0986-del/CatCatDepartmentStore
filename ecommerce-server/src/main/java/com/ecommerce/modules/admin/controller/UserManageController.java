package com.ecommerce.modules.admin.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.user.entity.User;
import com.ecommerce.modules.user.mapper.UserMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/user")
public class UserManageController {

    @Resource
    private UserMapper userMapper;

    @GetMapping("/list")
    public Result getUserList(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String username,
                              @RequestParam(required = false) String phone,
                              @RequestParam(required = false) String email,
                              @RequestParam(required = false) Integer status) {
        List<User> users;
        
        if (username != null || phone != null || email != null || status != null) {
            users = userMapper.searchUsers(username, phone, email, status);
        } else if (keyword != null && !keyword.isEmpty()) {
            users = userMapper.selectByKeyword(keyword);
        } else {
            users = userMapper.selectAll();
        }
        return Result.success(users);
    }

    @GetMapping("/{id}")
    public Result getUserById(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            return Result.success(user);
        }
        return Result.fail("用户不存在");
    }

    @PostMapping("/add")
    public Result addUser(@RequestBody User user) {
        userMapper.insert(user);
        return Result.success("添加成功");
    }

    @PostMapping("/update")
    public Result updateUser(@RequestBody Map<String, Object> params) {
        Long id = Long.parseLong(params.get("id").toString());
        String username = (String) params.get("username");
        String email = (String) params.get("email");
        String avatar = (String) params.get("avatar");
        String phone = params.get("phone") != null ? params.get("phone").toString() : null;
        String password = (String) params.get("password");
        
        if (password != null && !password.isEmpty()) {
            userMapper.updatePassword(id, password);
        }
        
        int result = userMapper.updateUserInfo(id, username, email, avatar);
        if (result > 0) {
            User user = userMapper.selectById(id);
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("phone", user.getPhone());
            userMap.put("email", user.getEmail());
            userMap.put("avatar", user.getAvatar());
            userMap.put("status", user.getStatus());
            return Result.success(userMap);
        } else {
            return Result.fail("更新失败");
        }
    }

    @PostMapping("/updateStatus")
    public Result updateUserStatus(@RequestBody Map<String, Object> params) {
        Long userId = Long.parseLong(params.get("userId").toString());
        Object statusObj = params.get("status");
        Integer status;
        if (statusObj instanceof Integer) {
            status = (Integer) statusObj;
        } else {
            status = Integer.parseInt(statusObj.toString());
        }
        userMapper.updateStatus(userId, status);
        return Result.success("更新成功");
    }

    @DeleteMapping("/delete")
    public Result deleteUser(@RequestParam Long userId) {
        userMapper.delete(userId);
        return Result.success("删除成功");
    }

    @PostMapping("/uploadAvatar")
    public Result uploadAvatar(MultipartFile file) {
        try {
            String filePath = saveFile(file, "avatars");
            return Result.success(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error(500, "上传失败");
        }
    }

    private String saveFile(MultipartFile file, String folder) throws IOException {
        String resourcePath = System.getProperty("user.dir") + "/src/main/resources/static/" + folder;
        File dir = new File(resourcePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        String fileName = originalFilename;

        File dest = new File(dir, fileName);
        if (dest.exists()) {
            String nameWithoutExt = originalFilename.substring(0, originalFilename.lastIndexOf("."));
            String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            fileName = nameWithoutExt + "_" + System.currentTimeMillis() + ext;
            dest = new File(dir, fileName);
        }

        file.transferTo(dest);
        return folder + "/" + fileName;
    }

    @PostMapping("/batchUpdateStatus")
    public Result batchUpdateStatus(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Object> idList = (List<Object>) params.get("ids");
        Integer status = Integer.parseInt(params.get("status").toString());
        
        int successCount = 0;
        for (Object idObj : idList) {
            Long userId = Long.parseLong(idObj.toString());
            userMapper.updateStatus(userId, status);
            successCount++;
        }
        
        String statusText = status == 1 ? "启用" : "禁用";
        return Result.success(String.format("批量%s完成，成功%d个", statusText, successCount));
    }
}
