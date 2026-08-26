package van.project.wechatter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import van.project.wechatter.entity.Reminder;
import van.project.wechatter.entity.enums.ReminderStatus;
import van.project.wechatter.mapper.ReminderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderMapper reminderMapper;
    private final ReminderSchedulerService schedulerService;

    /** 新增提醒，并加入调度 */
    @Transactional
    public void add(Reminder reminder) {
        reminder.setStatus(ReminderStatus.ACTIVE);
        reminder.setCreateTime(LocalDateTime.now());
        reminder.setUpdateTime(LocalDateTime.now());
        reminderMapper.insert(reminder);
        schedulerService.schedule(reminder);
        log.info("Reminder added: id={}, openId={}, type={}, content={}",
                reminder.getId(), reminder.getOpenId(), reminder.getReminderType(), reminder.getContent());
    }

    /** 查询某用户所有启用中的提醒 */
    public List<Reminder> listActiveByOpenId(String openId) {
        LambdaQueryWrapper<Reminder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reminder::getOpenId, openId)
               .eq(Reminder::getStatus, ReminderStatus.ACTIVE)
               .orderByDesc(Reminder::getCreateTime);
        return reminderMapper.selectList(wrapper);
    }

    /** 查询所有启用中的提醒 */
    public List<Reminder> listAllActive() {
        LambdaQueryWrapper<Reminder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reminder::getStatus, ReminderStatus.ACTIVE)
               .orderByDesc(Reminder::getCreateTime);
        return reminderMapper.selectList(wrapper);
    }

    /** 取消提醒：置为已取消状态，移除定时调度 */
    @Transactional
    public void cancel(Long id) {
        Reminder reminder = reminderMapper.selectById(id);
        if (reminder == null) {
            log.warn("Cancel failed: reminder [{}] not found", id);
            return;
        }
        if (reminder.getStatus() != ReminderStatus.ACTIVE) {
            log.warn("Cancel failed: reminder [{}] already {}", id, reminder.getStatus().getValue());
            return;
        }
        reminder.setStatus(ReminderStatus.CANCELLED);
        reminder.setUpdateTime(LocalDateTime.now());
        reminderMapper.updateById(reminder);
        schedulerService.cancelScheduled(id);
        log.info("Reminder cancelled: id={}", id);
    }
}