package com.library.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.library.common.Result;
import com.library.entity.User;
import com.library.mapper.UserMapper;
import com.library.service.TokenService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserMapper mapper;
    private final TokenService tokens;

    public AuthController(UserMapper mapper, TokenService tokens) {
        this.mapper = mapper;
        this.tokens = tokens;
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> body) {
        User u = mapper.selectOne(new QueryWrapper<User>().eq("username", body.get("username")));
        if (u == null || !u.getPassword().equals(body.get("password"))) {
            return Result.fail("用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(u.getStatus())) return Result.fail("账号已禁用");
        u.setPassword(null);
        return Result.ok("登录成功", Map.of("user", u, "token", tokens.create(u)));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value="Authorization", required=false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            tokens.remove(authorization.substring(7));
        }
        return Result.ok(null);
    }
}
