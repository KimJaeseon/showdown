package com.showdown.backend.api;

import com.showdown.backend.api.dto.ScheduleDtos.KnockoutGenerateRequest;
import com.showdown.backend.api.dto.ScheduleDtos.RoundRobinGenerateRequest;
import com.showdown.backend.api.dto.ScheduleDtos.ScheduleGenerateResponse;
import com.showdown.backend.api.dto.ScheduleDtos.ScheduleReportResponse;
import com.showdown.backend.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tournaments/{tournamentId}/schedule")
@Tag(name = "Schedule", description = "대진 자동 생성 및 검증 리포트 API")
public class ScheduleController {
    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping("/round-robin")
    @Operation(summary = "조 편성 및 조별리그 대진 자동 생성")
    public ScheduleGenerateResponse generateRoundRobin(@PathVariable UUID tournamentId, @RequestBody RoundRobinGenerateRequest request) {
        return scheduleService.generateRoundRobin(tournamentId, request);
    }

    @PostMapping("/knockout")
    @Operation(summary = "토너먼트 첫 라운드 대진 생성")
    public ScheduleGenerateResponse generateKnockout(@PathVariable UUID tournamentId, @RequestBody KnockoutGenerateRequest request) {
        return scheduleService.generateKnockout(tournamentId, request);
    }

    @GetMapping("/report")
    @Operation(summary = "경기 수, 코트/심판 충돌, 심판 누락 요약")
    public ScheduleReportResponse report(@PathVariable UUID tournamentId) {
        return scheduleService.report(tournamentId);
    }
}
