package com.library.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("library")
public class Library {
    @TableId(type = IdType.AUTO) private Long id;
    private String name;
    private String address;
    private String description;
    private Integer status;
    private LocalDateTime createdTime;
}
