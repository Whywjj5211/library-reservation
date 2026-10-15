package com.library.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.library.common.Result;
import com.library.entity.*;
import com.library.mapper.*;
import com.library.service.TokenService;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserMapper users;
    private final LibraryMapper libraries;
    private final ReadingRoomMapper rooms;
    private final SeatMapper seats;
    private final ReservationMapper reservations;
    private final ViolationMapper violations;
    private final NoticeMapper notices;
    private final TokenService tokenService;

    public AdminController(UserMapper users, LibraryMapper libraries, ReadingRoomMapper rooms,
                           SeatMapper seats, ReservationMapper reservations, ViolationMapper violations,
                           NoticeMapper notices, TokenService tokenService) {
        this.users=users; this.libraries=libraries; this.rooms=rooms; this.seats=seats;
        this.reservations=reservations; this.violations=violations; this.notices=notices;
        this.tokenService=tokenService;
    }

    private User admin(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        User u = tokenService.get(authorization.substring(7));
        return u != null && "ADMIN".equals(u.getRole()) ? u : null;
    }
    private <T> Result<T> denied() { return Result.fail("没有管理员权限，请重新登录"); }

    @GetMapping("/stats")
    public Result<?> stats(@RequestHeader(value="Authorization",required=false) String auth) {
        if (admin(auth)==null) return denied();
        LocalDate today=LocalDate.now();
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("users", users.selectCount(new QueryWrapper<>()));
        m.put("libraries", libraries.selectCount(new QueryWrapper<Library>().eq("status",1)));
        m.put("rooms", rooms.selectCount(new QueryWrapper<ReadingRoom>().eq("status",1)));
        m.put("seats", seats.selectCount(new QueryWrapper<Seat>().eq("status",1)));
        m.put("todayReservations", reservations.selectCount(new QueryWrapper<Reservation>().eq("reservation_date",today)
                .in("status",List.of("RESERVED","CHECKED_IN","COMPLETED"))));
        m.put("todayCheckIn", reservations.selectCount(new QueryWrapper<Reservation>().eq("reservation_date",today)
                .eq("status","CHECKED_IN")));
        m.put("activeReservations", reservations.selectCount(new QueryWrapper<Reservation>().in("status",List.of("RESERVED","CHECKED_IN"))));
        List<Map<String,Object>> trend=new ArrayList<>();
        for(int i=6;i>=0;i--){
            LocalDate d=today.minusDays(i);
            long c=reservations.selectCount(new QueryWrapper<Reservation>().eq("reservation_date",d));
            trend.add(Map.of("date",d.toString().substring(5),"count",c));
        }
        m.put("trend",trend);
        return Result.ok(m);
    }

    @GetMapping("/users")
    public Result<?> userList(@RequestHeader(value="Authorization",required=false) String auth) {
        if(admin(auth)==null) return denied();
        List<User> list=users.selectList(new QueryWrapper<User>().orderByDesc("created_time"));
        list.forEach(u->u.setPassword(null));
        return Result.ok(list);
    }

    @PutMapping("/users/{id}/status")
    public Result<?> userStatus(@RequestHeader(value="Authorization",required=false) String auth,
                                @PathVariable Long id,@RequestParam Integer status) {
        if(admin(auth)==null) return denied();
        User u=users.selectById(id); if(u==null) return Result.fail("用户不存在");
        u.setStatus(status); users.updateById(u); return Result.ok(null);
    }

    @GetMapping("/libraries")
    public Result<?> libraryList(@RequestHeader(value="Authorization",required=false) String auth) {
        if(admin(auth)==null) return denied();
        return Result.ok(libraries.selectList(new QueryWrapper<Library>().orderByDesc("id")));
    }

    @PostMapping("/libraries")
    public Result<?> libraryAdd(@RequestHeader(value="Authorization",required=false) String auth,@RequestBody Library x) {
        if(admin(auth)==null) return denied(); x.setStatus(1); libraries.insert(x); return Result.ok(x);
    }

    @PutMapping("/libraries/{id}")
    public Result<?> libraryUpdate(@RequestHeader(value="Authorization",required=false) String auth,@PathVariable Long id,@RequestBody Library x) {
        if(admin(auth)==null) return denied(); x.setId(id); libraries.updateById(x); return Result.ok(x);
    }

    @DeleteMapping("/libraries/{id}")
    public Result<?> libraryDelete(@RequestHeader(value="Authorization",required=false) String auth,@PathVariable Long id) {
        if(admin(auth)==null) return denied(); libraries.deleteById(id); return Result.ok(null);
    }

    @GetMapping("/rooms")
    public Result<?> roomList(@RequestHeader(value="Authorization",required=false) String auth) {
        if(admin(auth)==null) return denied();
        return Result.ok(rooms.selectList(new QueryWrapper<ReadingRoom>().orderByDesc("id")));
    }

    @PostMapping("/rooms")
    public Result<?> roomAdd(@RequestHeader(value="Authorization",required=false) String auth,@RequestBody ReadingRoom x) {
        if(admin(auth)==null) return denied(); x.setStatus(1); rooms.insert(x); return Result.ok(x);
    }

    @PutMapping("/rooms/{id}")
    public Result<?> roomUpdate(@RequestHeader(value="Authorization",required=false) String auth,@PathVariable Long id,@RequestBody ReadingRoom x) {
        if(admin(auth)==null) return denied(); x.setId(id); rooms.updateById(x); return Result.ok(x);
    }

    @DeleteMapping("/rooms/{id}")
    public Result<?> roomDelete(@RequestHeader(value="Authorization",required=false) String auth,@PathVariable Long id) {
        if(admin(auth)==null) return denied(); rooms.deleteById(id); return Result.ok(null);
    }

    @GetMapping("/seats")
    public Result<?> seatList(@RequestHeader(value="Authorization",required=false) String auth) {
        if(admin(auth)==null) return denied();
        return Result.ok(seats.selectList(new QueryWrapper<Seat>().orderByAsc("room_id","row_no","column_no")));
    }

    @PostMapping("/seats")
    public Result<?> seatAdd(@RequestHeader(value="Authorization",required=false) String auth,@RequestBody Seat x) {
        if(admin(auth)==null) return denied(); x.setStatus(1); seats.insert(x); return Result.ok(x);
    }

    @PutMapping("/seats/{id}")
    public Result<?> seatUpdate(@RequestHeader(value="Authorization",required=false) String auth,@PathVariable Long id,@RequestBody Seat x) {
        if(admin(auth)==null) return denied(); x.setId(id); seats.updateById(x); return Result.ok(x);
    }

    @DeleteMapping("/seats/{id}")
    public Result<?> seatDelete(@RequestHeader(value="Authorization",required=false) String auth,@PathVariable Long id) {
        if(admin(auth)==null) return denied(); seats.deleteById(id); return Result.ok(null);
    }

    @GetMapping("/reservations")
    public Result<?> reservationList(@RequestHeader(value="Authorization",required=false) String auth) {
        if(admin(auth)==null) return denied();
        return Result.ok(reservations.selectList(new QueryWrapper<Reservation>().orderByDesc("created_time")));
    }

    @GetMapping("/violations")
    public Result<?> violationList(@RequestHeader(value="Authorization",required=false) String auth) {
        if(admin(auth)==null) return denied();
        return Result.ok(violations.selectList(new QueryWrapper<Violation>().orderByDesc("created_time")));
    }

    @PutMapping("/violations/{id}/resolve")
    public Result<?> resolveViolation(@RequestHeader(value="Authorization",required=false) String auth,@PathVariable Long id) {
        if(admin(auth)==null) return denied();
        Violation v=violations.selectById(id); if(v==null) return Result.fail("记录不存在");
        v.setStatus(0); violations.updateById(v); return Result.ok(null);
    }

    @GetMapping("/notices")
    public Result<?> noticeList(@RequestHeader(value="Authorization",required=false) String auth) {
        if(admin(auth)==null) return denied();
        return Result.ok(notices.selectList(new QueryWrapper<Notice>().orderByDesc("created_time")));
    }

    @PostMapping("/notices")
    public Result<?> noticeAdd(@RequestHeader(value="Authorization",required=false) String auth,@RequestBody Notice x) {
        User a=admin(auth); if(a==null) return denied();
        x.setPublisherId(a.getId()); notices.insert(x); return Result.ok(x);
    }

    @PutMapping("/notices/{id}")
    public Result<?> noticeUpdate(@RequestHeader(value="Authorization",required=false) String auth,@PathVariable Long id,@RequestBody Notice x) {
        if(admin(auth)==null) return denied(); x.setId(id); notices.updateById(x); return Result.ok(x);
    }

    @DeleteMapping("/notices/{id}")
    public Result<?> noticeDelete(@RequestHeader(value="Authorization",required=false) String auth,@PathVariable Long id) {
        if(admin(auth)==null) return denied(); notices.deleteById(id); return Result.ok(null);
    }
}
