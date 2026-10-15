package com.library.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("violation")
public class Violation {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private Long reservationId;
    private String reason;
    private Integer status;
    private LocalDateTime createdTime;
}
