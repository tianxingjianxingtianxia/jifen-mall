package com.jifen.modules.points;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jifen.auth.User;
import com.jifen.auth.UserMapper;
import com.jifen.common.PageResult;
import com.jifen.common.exception.BusinessException;
import com.jifen.modules.points.dto.BalanceResponse;
import com.jifen.modules.points.dto.PointRecordVO;
import com.jifen.modules.points.dto.SignInResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements PointsService {

    private final UserMapper userMapper;
    private final PointSignInMapper pointSignInMapper;
    private final PointRecordMapper pointRecordMapper;
    private final SysConfigMapper sysConfigMapper;

    @Override
    @Transactional
    public SignInResponse signIn(Long userId) {
        LocalDate today = LocalDate.now();

        // Check if already signed in today
        int count = pointSignInMapper.countByUserIdAndDate(userId, today);
        if (count > 0) {
            throw new BusinessException("今日已签到");
        }

        // Get sign-in points from config
        int signInPoints = getSignInPoints();

        // Get current user info
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // Update user points (UPDATE statement)
        userMapper.addPointsAndEarned(userId, signInPoints);

        // Record sign-in
        PointSignIn signIn = new PointSignIn();
        signIn.setUserId(userId);
        signIn.setSignDate(today);
        signIn.setPointsAwarded(signInPoints);
        pointSignInMapper.insert(signIn);

        // Record point detail - set expiry_date based on config
        PointRecord record = new PointRecord();
        record.setUserId(userId);
        record.setType(1); // 获得
        record.setSource("SIGN_IN");
        record.setPoints(signInPoints);
        record.setBalanceBefore(user.getPoints());
        record.setBalanceAfter(user.getPoints() + signInPoints);
        record.setRelatedId(signIn.getId());
        record.setRemark("每日签到");
        // 设置积分过期时间
        int validityDays = getPointsValidityDays();
        if (validityDays > 0) {
            record.setExpireTime(LocalDateTime.now().plusDays(validityDays));
        }
        pointRecordMapper.insert(record);

        // Return response
        return new SignInResponse(true, signInPoints, user.getPoints() + signInPoints);
    }

    /**
     * 从系统配置获取积分有效期天数，默认365
     */
    private int getPointsValidityDays() {
        String value = sysConfigMapper.getValueByKey("points_validity_days");
        if (value == null) {
            return 365;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("points_validity_days 配置值无效: {}", value);
            return 365;
        }
    }

    @Override
    public boolean isTodaySigned(Long userId) {
        LocalDate today = LocalDate.now();
        return pointSignInMapper.countByUserIdAndDate(userId, today) > 0;
    }

    @Override
    public BalanceResponse getBalance(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return new BalanceResponse(
            user.getPoints(),
            user.getTotalEarned(),
            user.getTotalSpent()
        );
    }

    @Override
    public PageResult<PointRecordVO> getRecords(Long userId, int pageNum, int pageSize) {
        // Build query
        LambdaQueryWrapper<PointRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointRecord::getUserId, userId)
               .orderByDesc(PointRecord::getCreateTime);

        // Paginate
        IPage<PointRecord> page = pointRecordMapper.selectPage(
            new Page<>(pageNum, pageSize), wrapper
        );

        // Convert to VO
        IPage<PointRecordVO> voPage = page.convert(this::toPointRecordVO);

        return PageResult.of(voPage);
    }

    private PointRecordVO toPointRecordVO(PointRecord record) {
        PointRecordVO vo = new PointRecordVO();
        vo.setId(record.getId());
        vo.setPoints(record.getPoints());
        vo.setType(record.getType());
        vo.setSource(record.getSource());
        vo.setRemark(record.getRemark());
        vo.setCreateTime(record.getCreateTime());
        vo.setExpireTime(record.getExpireTime());
        return vo;
    }

    /**
     * 从系统配置获取签到奖励积分，默认10
     */
    private int getSignInPoints() {
        String value = sysConfigMapper.getValueByKey("sign_in_points");
        if (value == null) {
            return 10;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("sign_in_points 配置值无效: {}", value);
            return 10;
        }
    }

    @Override
    @Transactional
    public void topup(Long userId, int points) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        userMapper.addPointsAndEarned(userId, points);
        log.info("[TEST] 用户{}充值{}积分", userId, points);
    }
}
