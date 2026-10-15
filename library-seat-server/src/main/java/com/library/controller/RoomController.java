package com.library.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.library.common.Result;
import com.library.entity.ReadingRoom;
import com.library.mapper.ReadingRoomMapper;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final ReadingRoomMapper mapper;
    public RoomController(ReadingRoomMapper mapper) { this.mapper = mapper; }

    @GetMapping
    public Result<List<ReadingRoom>> list(@RequestParam(required=false) Long libraryId) {
        QueryWrapper<ReadingRoom> q = new QueryWrapper<ReadingRoom>().eq("status", 1);
        if (libraryId != null) q.eq("library_id", libraryId);
        return Result.ok(mapper.selectList(q.orderByAsc("floor","id")));
    }
}
