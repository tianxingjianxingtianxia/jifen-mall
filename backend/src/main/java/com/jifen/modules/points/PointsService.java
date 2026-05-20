package com.jifen.modules.points;

import com.jifen.common.PageResult;
import com.jifen.modules.points.dto.BalanceResponse;
import com.jifen.modules.points.dto.PointRecordVO;
import com.jifen.modules.points.dto.SignInResponse;

public interface PointsService {

    /**
     * 每日签到
     */
    SignInResponse signIn(Long userId);

    /**
     * 查询今日是否已签到
     */
    boolean isTodaySigned(Long userId);

    /**
     * 查询积分余额
     */
    BalanceResponse getBalance(Long userId);

    /**
     * 积分明细分页查询（时间倒序）
     */
    PageResult<PointRecordVO> getRecords(Long userId, int pageNum, int pageSize);

    /**
     * [测试专用] 充值积分
     */
    void topup(Long userId, int points);
}
