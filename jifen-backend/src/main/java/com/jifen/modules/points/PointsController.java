package com.jifen.modules.points;

import com.jifen.auth.UserContextUtil;
import com.jifen.common.PageResult;
import com.jifen.common.Result;
import com.jifen.modules.points.dto.BalanceResponse;
import com.jifen.modules.points.dto.PointRecordVO;
import com.jifen.modules.points.dto.SignInResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;

    /**
     * 每日签到
     */
    @PostMapping("/sign-in")
    public Result<SignInResponse> signIn() {
        Long userId = UserContextUtil.getUserId();
        SignInResponse response = pointsService.signIn(userId);
        return Result.success(response);
    }

    /**
     * 查询今日是否已签到
     */
    @GetMapping("/today-sign")
    public Result<Boolean> todaySign() {
        Long userId = UserContextUtil.getUserId();
        boolean signed = pointsService.isTodaySigned(userId);
        return Result.success(signed);
    }

    /**
     * 查询积分余额/汇总
     */
    @GetMapping("/balance")
    public Result<BalanceResponse> balance() {
        Long userId = UserContextUtil.getUserId();
        BalanceResponse response = pointsService.getBalance(userId);
        return Result.success(response);
    }

    /**
     * 积分明细（分页）
     */
    @GetMapping("/records")
    public Result<PageResult<PointRecordVO>> records(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = UserContextUtil.getUserId();
        PageResult<PointRecordVO> page = pointsService.getRecords(userId, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * [测试专用] 充值积分 (仅在dev/ci环境使用)
     */
    @PostMapping("/topup")
    public Result<Void> topup(@RequestParam int points) {
        Long userId = UserContextUtil.getUserId();
        pointsService.topup(userId, points);
        return Result.success();
    }
}
