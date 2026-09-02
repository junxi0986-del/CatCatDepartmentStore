package com.ecommerce.modules.user.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.coupon.entity.Coupon;
import com.ecommerce.modules.coupon.entity.UserCoupon;
import com.ecommerce.modules.coupon.mapper.CouponMapper;
import com.ecommerce.modules.coupon.mapper.UserCouponMapper;
import com.ecommerce.modules.user.entity.User;
import com.ecommerce.modules.user.mapper.UserMapper;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserCouponMapper userCouponMapper;

    @Resource
    private CouponMapper couponMapper;
    
    private static final Map<String, String> captchaStore = new ConcurrentHashMap<>();
    private static final Random random = new Random();

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String phone = params.get("phone");
        String password = params.get("password");

        User user = null;
        String loginAccount = "";
        
        if (username != null && !username.isEmpty()) {
            loginAccount = "用户名";
            user = userMapper.selectByUsername(username);
        } else if (phone != null && !phone.isEmpty()) {
            loginAccount = "手机号";
            user = userMapper.selectByPhone(phone);
        }

        if (user == null) {
            return Result.fail(loginAccount + "不存在");
        }
        
        if (!user.getPassword().equals(password)) {
            return Result.fail("密码错误");
        }
        
        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.fail("账号已被禁用，请联系客服");
        }

        String token = UUID.randomUUID().toString() + "_" + user.getId();
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("phone", user.getPhone());
        userMap.put("email", user.getEmail());
        userMap.put("avatar", user.getAvatar());
        data.put("user", userMap);
        return Result.success(data);
    }

    @GetMapping("/captcha")
    public Result generateCaptcha() {
        String captchaId = UUID.randomUUID().toString();
        int a = random.nextInt(10) + 1;
        int b = random.nextInt(10) + 1;
        int operator = random.nextInt(3);
        String question;
        String answer;
        
        switch (operator) {
            case 0:
                question = a + " + " + b + " = ?";
                answer = String.valueOf(a + b);
                break;
            case 1:
                if (a < b) { int temp = a; a = b; b = temp; }
                question = a + " - " + b + " = ?";
                answer = String.valueOf(a - b);
                break;
            default:
                a = random.nextInt(9) + 1;
                b = random.nextInt(9) + 1;
                question = a + " × " + b + " = ?";
                answer = String.valueOf(a * b);
                break;
        }
        
        captchaStore.put(captchaId, answer);
        
        try {
            BufferedImage image = generateCaptchaImage(question);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", baos);
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
            
            Map<String, Object> data = new HashMap<>();
            data.put("captchaId", captchaId);
            data.put("image", "data:image/png;base64," + base64Image);
            return Result.success(data);
        } catch (Exception e) {
            return Result.fail("生成验证码失败");
        }
    }
    
    private BufferedImage generateCaptchaImage(String text) {
        int width = 120;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, width, height);
        
        g.setColor(new Color(50, 50, 50));
        g.setFont(new Font("Arial", Font.BOLD, 20));
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (width - textWidth) / 2, 28);
        
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(random.nextInt(150), random.nextInt(150), random.nextInt(150)));
            g.drawLine(random.nextInt(width), random.nextInt(height), 
                       random.nextInt(width), random.nextInt(height));
        }
        
        g.dispose();
        return image;
    }

    @PostMapping("/register")
    public Result register(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        String phone = params.get("phone");
        String email = params.get("email");
        String captchaId = params.get("captchaId");
        String captchaAnswer = params.get("captchaAnswer");

        if (username == null || username.trim().isEmpty()) {
            return Result.fail("用户名不能为空");
        }
        
        if (password == null || password.trim().isEmpty()) {
            return Result.fail("密码不能为空");
        }
        
        if (password.trim().length() < 6) {
            return Result.fail("密码长度不能少于6位");
        }
        
        if (phone == null || phone.trim().isEmpty()) {
            return Result.fail("手机号不能为空");
        }
        
        if (captchaId == null || captchaAnswer == null || captchaAnswer.trim().isEmpty()) {
            return Result.fail("请完成人机验证");
        }
        
        String correctAnswer = captchaStore.get(captchaId);
        if (correctAnswer == null) {
            return Result.fail("验证码已过期，请刷新");
        }
        
        if (!correctAnswer.equals(captchaAnswer.trim())) {
            captchaStore.remove(captchaId);
            return Result.fail("验证码错误");
        }
        
        captchaStore.remove(captchaId);

        User existingUser = userMapper.selectByUsername(username.trim());
        if (existingUser != null) {
            return Result.fail("用户名已存在");
        }

        User existingPhone = userMapper.selectByPhone(phone.trim());
        if (existingPhone != null) {
            return Result.fail("手机号已被注册");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(password.trim());
        user.setPhone(phone.trim());
        if (email != null && !email.trim().isEmpty()) {
            user.setEmail(email.trim());
        }
        user.setAvatar("https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=user%20avatar&image_size=square");
        userMapper.insert(user);
        return Result.success("注册成功");
    }

    @GetMapping("/info")
    public Result getUserInfo(@RequestHeader(value = "token", required = false) String token) {
        // 从token中获取用户ID
        Long userId = getUserIdFromToken(token);
        User user = userMapper.selectById(userId);
        if (user != null) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("phone", user.getPhone());
            userMap.put("email", user.getEmail());
            userMap.put("avatar", user.getAvatar());
            return Result.success(userMap);
        } else {
            return Result.fail("用户不存在");
        }
    }

    @PostMapping("/update")
    public Result update(@RequestBody Map<String, Object> params, @RequestHeader(value = "token", required = false) String token) {
        Long userId = getUserIdFromToken(token);
        String username = (String) params.get("username");
        String email = (String) params.get("email");
        String avatar = (String) params.get("avatar");
        String phone = params.get("phone") != null ? params.get("phone").toString() : null;

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        int result = userMapper.updateUserInfo(userId, username, email, avatar);
        if (result > 0) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", username);
            userMap.put("phone", phone != null ? phone : user.getPhone());
            userMap.put("email", email);
            userMap.put("avatar", avatar);
            return Result.success(userMap);
        } else {
            return Result.fail("更新失败");
        }
    }

    @PostMapping("/changePassword")
    public Result changePassword(@RequestBody Map<String, String> params, @RequestHeader(value = "token", required = false) String token) {
        // 从token中获取用户ID
        Long userId = getUserIdFromToken(token);
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            return Result.fail("参数不能为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        // 验证旧密码
        if (!user.getPassword().equals(oldPassword)) {
            return Result.fail("旧密码错误");
        }

        // 更新新密码
        user.setPassword(newPassword);
        int result = userMapper.update(user);
        if (result > 0) {
            return Result.success("密码修改成功");
        } else {
            return Result.fail("密码修改失败");
        }
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
        return 1L;
    }

    @GetMapping("/coupons")
    public Result getUserCoupons(@RequestHeader(value = "token", required = false) String token) {
        // 从token中获取用户ID
        Long userId = getUserIdFromToken(token);
        // 获取用户的优惠券列表
        List<Map<String, Object>> userCoupons = userCouponMapper.selectByUserIdWithDetails(userId);
        return Result.success(userCoupons);
    }

    @PostMapping("/uploadAvatar")
    public Result uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String filePath = saveFile(file);
            return Result.success(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error(500, "上传失败");
        }
    }

    private String saveFile(MultipartFile file) throws IOException {
        String resourcePath = "D:/Desktop/ecommerce-platform/ecommerce-server/src/main/resources/static/userimages";
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
        return "userimages/" + fileName;
    }

    @PostMapping("/claimCoupon")
    public Result claimCoupon(@RequestHeader(value = "token", required = false) String token,
                              @RequestBody Map<String, Object> params) {
        // 从token中获取用户ID
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return Result.fail("用户未登录");
        }

        // 获取优惠券ID
        Object couponIdObj = params.get("couponId");
        if (couponIdObj == null) {
            return Result.fail("优惠券ID不能为空");
        }
        Long couponId = Long.parseLong(couponIdObj.toString());

        // 查询优惠券信息
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            return Result.fail("优惠券不存在");
        }

        // 检查优惠券状态（status=1表示启用，status=0表示禁用）
        if (coupon.getStatus() != 1) {
            return Result.fail("优惠券已禁用");
        }

        // 检查优惠券时间范围
        Date now = new Date();
        if (coupon.getStartTime() != null && coupon.getStartTime().after(now)) {
            return Result.fail("优惠券尚未生效");
        }
        if (coupon.getEndTime() != null && coupon.getEndTime().before(now)) {
            return Result.fail("优惠券已过期");
        }

        // 检查是否还有剩余数量
        if (coupon.getUsedCount() >= coupon.getTotalCount()) {
            return Result.fail("优惠券已被领完");
        }

        // 检查用户是否已经领取过该优惠券
        List<UserCoupon> existingCoupons = userCouponMapper.selectByUserIdAndStatus(userId, 0);
        boolean alreadyClaimed = existingCoupons.stream()
                .anyMatch(uc -> uc.getCouponId().equals(couponId));
        if (alreadyClaimed) {
            return Result.fail("您已经领取过该优惠券");
        }

        // 创建用户优惠券记录
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setCouponId(couponId);
        userCoupon.setUserId(userId);
        userCoupon.setStatus(0); // 状态为可使用
        userCoupon.setCreateTime(new Date());

        int result = userCouponMapper.insert(userCoupon);
        if (result > 0) {
            // 更新优惠券发放数量
            couponMapper.incrementUsedCount(couponId);
            return Result.success("领取成功");
        } else {
            return Result.error(500, "领取失败");
        }
    }

}