package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.library.entity.Reservation;
import com.library.entity.Violation;
import com.library.mapper.ReservationMapper;
import com.library.mapper.ViolationMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.List;

@Component
public class ReservationScheduler {
    private final ReservationMapper reservations;
    private final ViolationMapper violations;

    public ReservationScheduler(ReservationMapper reservations, ViolationMapper violations) {
        this.reservations=reservations; this.violations=violations;
    }

    @Scheduled(fixedDelay = 300000) // 任务结束后，隔5分钟再跑
    public void releaseExpiredReservations() {
        LocalDate today = LocalDate.now();
        LocalTime timeLimit = LocalTime.now().minusMinutes(30);

        QueryWrapper<Reservation> wrapper = new QueryWrapper<>();
        wrapper.eq("reservation_date", today);
        wrapper.eq("status", "RESERVED");
        wrapper.lt("start_time", timeLimit);

        List<Reservation> list = reservations.selectList(wrapper);

        for (Reservation r : list) {
            r.setStatus("NO_SHOW");
            reservations.updateById(r);
        }
    }}

