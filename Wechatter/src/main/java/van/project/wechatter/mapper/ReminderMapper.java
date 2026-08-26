package van.project.wechatter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import van.project.wechatter.entity.Reminder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReminderMapper extends BaseMapper<Reminder> {
}