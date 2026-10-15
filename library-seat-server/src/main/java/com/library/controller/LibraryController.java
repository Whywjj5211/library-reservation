package com.library.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.library.common.Result;
import com.library.entity.Library;
import com.library.mapper.LibraryMapper;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/libraries")
public class LibraryController {
    private final LibraryMapper mapper;
    public LibraryController(LibraryMapper mapper) { this.mapper = mapper; }

    @GetMapping
    public Result<List<Library>> list() {
        return Result.ok(mapper.selectList(new QueryWrapper<Library>().eq("status", 1)));
    }
}
