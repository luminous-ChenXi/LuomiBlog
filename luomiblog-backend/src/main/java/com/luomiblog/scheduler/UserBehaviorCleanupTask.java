package com.luomiblog.scheduler;

import com.luomiblog.entity.UserBehavior;
import com.luomiblog.repository.UserBehaviorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * user_behavior 流水每日清理任务。
 *
 * 浏览/点赞/搜索等行为流水只服务短期去重与统计（浏览 24 小时去重窗口），
 * 长期留存只会让表无限膨胀；此处按保留期每日分批删除，
 * 单次运行有总删除上限保护，避免历史积压时一次删爆。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserBehaviorCleanupTask {

    /** 流水保留天数 */
    public static final int RETENTION_DAYS = 90;

    /** 单次运行删除总量上限（保护：历史积压时不无限删） */
    public static final long MAX_DELETE_PER_RUN = 50_000L;

    /** 每批删除条数 */
    private static final int BATCH_SIZE = 500;

    private final UserBehaviorRepository userBehaviorRepository;

    /**
     * 每日 03:30 执行
     */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void cleanupExpiredBehaviors() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        long deletedTotal = 0;
        int batches = 0;

        while (deletedTotal < MAX_DELETE_PER_RUN) {
            var batch = userBehaviorRepository
                    .findTop500ByCreatedAtBeforeOrderByCreatedAtAsc(cutoff);
            if (batch.isEmpty()) {
                break;
            }
            userBehaviorRepository.deleteAllInBatch(batch);
            deletedTotal += batch.size();
            batches++;
        }

        if (deletedTotal > 0) {
            log.info("user_behavior 清理完成: cutoff={}, 删除 {} 条（{} 批）", cutoff, deletedTotal, batches);
            if (deletedTotal >= MAX_DELETE_PER_RUN) {
                log.warn("user_behavior 本次清理达到单次上限 {} 条，剩余积压明日继续", MAX_DELETE_PER_RUN);
            }
        } else {
            log.debug("user_behavior 无过期流水需要清理: cutoff={}", cutoff);
        }
    }
}
