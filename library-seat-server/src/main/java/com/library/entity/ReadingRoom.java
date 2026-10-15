package com.library.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("reading_room")
public class ReadingRoom {
    @TableId(type = IdType.AUTO) private Long id;
    private Long libraryId;
    private String name;
    private String floor;
    private Integer capacity;
    private Integer status;
    private LocalDateTime createdTime;
}
