package com.library.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.library.common.Result;
import com.library.entity.Seat;
import com.library.mapper.SeatMapper;
import com.library.mapper.ReservationMapper;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/seats")
public class SeatController {
    private final SeatMapper seats;
    private final ReservationMapper reservations;

    public SeatController(SeatMapper seats, ReservationMapper reservations) {
        this.seats = seats; this.reservations = reservations;
    }

    @GetMapping
    public Result<List<Seat>> list(@RequestParam(required=false) Long roomId) {
        QueryWrapper<Seat> q = new QueryWrapper<>();
        q.eq(roomId != null, "room_id", roomId).eq("status", 1)
         .orderByAsc("row_no","column_no");
        return Result.ok(seats.selectList(q));
    }

    @GetMapping("/occupied")
    public Result<List<Long>> occupied(@RequestParam Long roomId,
                                       @RequestParam LocalDate date,
                                       @RequestParam LocalTime startTime,
                                       @RequestParam LocalTime endTime) {
        List<Seat> roomSeats = seats.selectList(new QueryWrapper<Seat>()
                .eq("room_id", roomId).eq("status", 1));
        if (roomSeats.isEmpty()) return Result.ok(List.of());
        List<Long> ids = new ArrayList<>();
        for (Seat seat : roomSeats) {
            long count = reservations.selectCount(new QueryWrapper<com.library.entity.Reservation>()
                    .eq("seat_id", seat.getId())
                    .eq("reservation_date", date)
                    .in("status", List.of("RESERVED","CHECKED_IN"))
                    .lt("start_time", endTime)
                    .gt("end_time", startTime));
            if (count > 0) ids.add(seat.getId());
        }
        return Result.ok(ids);
    }
}
