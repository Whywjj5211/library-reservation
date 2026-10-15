package com.library.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.library.common.Result;
import com.library.entity.Notice;
import com.library.mapper.NoticeMapper;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {
    private final NoticeMapper mapper;
    public NoticeController(NoticeMapper mapper) { this.mapper = mapper; }

    @GetMapping
    public Result<List<Notice>> list() {
        return Result.ok(mapper.selectList(new QueryWrapper<Notice>().orderByDesc("created_time")));
    }
}
