package com.jifen.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jifen.auth.User;
import com.jifen.auth.UserMapper;
import com.jifen.modules.points.PointRecord;
import com.jifen.modules.points.PointRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

/**
 * 积分过期定时任务
 * 每年 12 月 31 日 23:59:59 执行
 * 规则：扣除当年获得的积分（不超过当前可用余额）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointExpireTask {

    private final UserMapper userMapper;
    private final PointRecordMapper pointRecordMapper;

    @Scheduled(cron = "59 59 23 31 12 ?")
    @Transactional
    public void expireYearlyPoints() {
        log.info("开始执行年度积分过期清理...");

        int year = Year.now().getValue();
        LocalDateTime yearStart = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime yearEnd = LocalDate.of(year, 12, 31).atTime(23, 59, 59);

        // 查询当前时间
        LocalDateTime now = LocalDateTime.now();

        // 只有在12月31日23:59:59之后才执行
        if (now.isBefore(yearEnd)) {
            log.info("未到积分过期时间，跳过");
            return;
        }

        // 获取所有活跃用户
        LambdaQueryWrapper<User> userWrapper = Wrappers.lambdaQuery(User.class)
                .eq(User::getStatus, 1);
        List<User> users = userMapper.selectList(userWrapper);

        int expiredCount = 0;
        for (User user : users) {
            if (user.getPoints() == null || user.getPoints() <= 0) {
                continue;
            }

            // 查询本年度获得的总积分
            LambdaQueryWrapper<PointRecord> earnedWrapper = Wrappers.lambdaQuery(PointRecord.class)
                    .eq(PointRecord::getUserId, user.getId())
                    .eq(PointRecord::getType, 1) // 获得
                    .ge(PointRecord::getCreateTime, yearStart)
                    .le(PointRecord::getCreateTime, yearEnd);
            List<PointRecord> earnedRecords = pointRecordMapper.selectList(earnedWrapper);
            int earnedThisYear = earnedRecords.stream()
                    .mapToInt(PointRecord::getPoints)
                    .sum();

            if (earnedThisYear <= 0) {
                continue;
            }

            // 扣除积分（不超过用户当前可用积分）
            int expirePoints = Math.min(earnedThisYear, user.getPoints());
            userMapper.expirePoints(user.getId(), expirePoints);

            // 记录积分过期明细
            PointRecord record = new PointRecord();
            record.setUserId(user.getId());
            record.setType(2); // 消耗
            record.setSource("EXPIRE");
            record.setPoints(expirePoints);
            record.setBalanceBefore(user.getPoints());
            record.setBalanceAfter(Math.max(user.getPoints() - expirePoints, 0));
            record.setRemark(year + "年度积分过期清理");
            pointRecordMapper.insert(record);

            expiredCount++;
            log.debug("用户 {} 积分过期 {}，当前积分 {}", user.getUsername(), expirePoints,
                    Math.max(user.getPoints() - expirePoints, 0));
        }

        log.info("年度积分过期清理完成，处理 {} 个用户", expiredCount);
    }
}
