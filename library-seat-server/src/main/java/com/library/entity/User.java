package com.library.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO) private Long id;
    private String username;
    private String password;
    private String studentNo;
    private String name;
    private String phone;
    private String role;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
