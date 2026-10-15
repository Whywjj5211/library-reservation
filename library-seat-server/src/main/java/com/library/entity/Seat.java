package com.library.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("seat")
public class Seat {
    @TableId(type = IdType.AUTO) private Long id;
    private Long roomId;
    private String seatNo;
    private Integer rowNo;
    private Integer columnNo;
    private Integer status;
    private LocalDateTime createdTime;
}
