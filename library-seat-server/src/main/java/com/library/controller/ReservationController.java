package com.library.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.library.common.Result;
import com.library.entity.Reservation;
import com.library.mapper.ReservationMapper;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    private final ReservationMapper mapper;
    public ReservationController(ReservationMapper mapper) { this.mapper = mapper; }

    @GetMapping
    public Result<List<Reservation>> list(@RequestParam Long userId) {
        return Result.ok(mapper.selectList(new QueryWrapper<Reservation>()
                .eq("user_id", userId).orderByDesc("created_time")));
    }

    @PostMapping
    public Result<Reservation> create(@RequestBody Reservation r) {
        if (r.getReservationDate()==null || r.getStartTime()==null || r.getEndTime()==null ||
            r.getSeatId()==null || r.getUserId()==null) return Result.fail("预约参数不完整");
        if (r.getReservationDate().isBefore(LocalDate.now())) return Result.fail("不能预约过去的日期");
        if (!r.getStartTime().isBefore(r.getEndTime())) return Result.fail("结束时间必须晚于开始时间");
        if (r.getStartTime().isBefore(LocalTime.of(8,0)) || r.getEndTime().isAfter(LocalTime.of(21,0)))
            return Result.fail("开放时间为 08:00 - 21:00");
        if (Duration.between(r.getStartTime(), r.getEndTime()).toHours() > 4)
            return Result.fail("单次预约最长 4 小时");

        QueryWrapper<Reservation> conflict = new QueryWrapper<Reservation>()
                .eq("seat_id", r.getSeatId())
                .eq("reservation_date", r.getReservationDate())
                .in("status", List.of("RESERVED","CHECKED_IN"))
                .lt("start_time", r.getEndTime()).gt("end_time", r.getStartTime());
        if (mapper.selectCount(conflict) > 0) return Result.fail("该座位在此时间段已被预约");

        QueryWrapper<Reservation> userConflict = new QueryWrapper<Reservation>()
                .eq("user_id", r.getUserId())
                .eq("reservation_date", r.getReservationDate())
                .in("status", List.of("RESERVED","CHECKED_IN"))
                .lt("start_time", r.getEndTime()).gt("end_time", r.getStartTime());
        if (mapper.selectCount(userConflict) > 0) return Result.fail("你在该时间段已有其他预约");

        long daily = mapper.selectCount(new QueryWrapper<Reservation>()
                .eq("user_id", r.getUserId()).eq("reservation_date", r.getReservationDate())
                .in("status", List.of("RESERVED","CHECKED_IN")));
        if (daily >= 3) return Result.fail("每天最多预约 3 次");

        r.setStatus("RESERVED");
        mapper.insert(r);
        return Result.ok("预约成功", r);
    }

    @DeleteMapping("/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        Reservation r = mapper.selectById(id);
        if (r == null) return Result.fail("预约不存在");
        if (!"RESERVED".equals(r.getStatus())) return Result.fail("当前预约不能取消");
        r.setStatus("CANCELLED"); mapper.updateById(r);
        return Result.ok(null);
    }

    @PostMapping("/{id}/check-in")
    public Result<Void> checkIn(@PathVariable Long id) {
        Reservation r = mapper.selectById(id);
        if (r == null) return Result.fail("预约不存在");
        if (!"RESERVED".equals(r.getStatus())) return Result.fail("当前状态不能签到");
        r.setStatus("CHECKED_IN"); r.setCheckInTime(LocalDateTime.now()); mapper.updateById(r);
        return Result.ok(null);
    }

    @PostMapping("/{id}/check-out")
    public Result<Void> checkOut(@PathVariable Long id) {
        Reservation r = mapper.selectById(id);
        if (r == null) return Result.fail("预约不存在");
        if (!"CHECKED_IN".equals(r.getStatus())) return Result.fail("请先签到");
        r.setStatus("COMPLETED"); r.setCheckOutTime(LocalDateTime.now()); mapper.updateById(r);
        return Result.ok(null);
    }
}
