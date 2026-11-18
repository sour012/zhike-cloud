package com.zhike.edu.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhike.common.BizException;
import com.zhike.edu.TimeSlot;
import com.zhike.edu.dto.EnrolledSlot;
import com.zhike.edu.dto.MyCourseVO;
import com.zhike.edu.entity.CourseOffering;
import com.zhike.edu.entity.Enrollment;
import com.zhike.edu.entity.Score;
import com.zhike.edu.entity.Term;
import com.zhike.edu.mapper.EnrollmentMapper;
import com.zhike.edu.mapper.OfferingMapper;
import com.zhike.edu.mapper.ScoreMapper;
import com.zhike.edu.mapper.TermMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 选课/退课。这里集中了三条必须成立的规则：
 *   1) 名额不超卖  —— 条件 UPDATE 原子占座，不先查后改
 *   2) 时间不冲突  —— 同一天节次区间有交集即拒绝
 *   3) 门数不超额  —— 一学期最多 8 门
 * 任一条不满足都抛 BizException，事务回滚时占座也一并回滚。
 */
@Service
public class EnrollmentService {

    /** 一学期选课上限，与公告里公示的规则保持一致 */
    public static final int MAX_COURSES_PER_TERM = 8;

    private final EnrollmentMapper enrollmentMapper;
    private final OfferingMapper offeringMapper;
    private final ScoreMapper scoreMapper;
    private final TermMapper termMapper;

    public EnrollmentService(EnrollmentMapper enrollmentMapper, OfferingMapper offeringMapper,
                             ScoreMapper scoreMapper, TermMapper termMapper) {
        this.enrollmentMapper = enrollmentMapper;
        this.offeringMapper = offeringMapper;
        this.scoreMapper = scoreMapper;
        this.termMapper = termMapper;
    }

    @Transactional
    public Long select(Long offeringId, Long studentId) {
        CourseOffering off = offeringMapper.selectById(offeringId);
        if (off == null) {
            throw new BizException(404, "开课记录不存在");
        }
        if (!Integer.valueOf(1).equals(off.getStatus())) {
            throw new BizException("该课程已关闭选课");
        }

        Enrollment exist = enrollmentMapper.selectOne(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getOfferingId, offeringId)
                .eq(Enrollment::getStudentId, studentId));
        if (exist != null && Integer.valueOf(1).equals(exist.getStatus())) {
            throw new BizException(409, "你已经选过这门课了");
        }

        List<EnrolledSlot> slots = enrollmentMapper.selectSlots(studentId, off.getTermId());
        for (EnrolledSlot slot : slots) {
            if (TimeSlot.overlap(off.getWeekday(), off.getStartSection(), off.getEndSection(),
                    slot.getWeekday(), slot.getStartSection(), slot.getEndSection())) {
                throw new BizException(409, "与《" + slot.getCourseName() + "》（"
                        + TimeSlot.text(slot.getWeekday(), slot.getStartSection(), slot.getEndSection())
                        + "）时间冲突");
            }
        }

        if (slots.size() >= MAX_COURSES_PER_TERM) {
            throw new BizException("本学期最多选修 " + MAX_COURSES_PER_TERM + " 门课程，请先退课");
        }

        // 原子占座：返回 0 说明已经满了，此时直接失败，不会出现「先查到有名额、写的时候没了」
        if (offeringMapper.tryOccupy(offeringId) == 0) {
            // 并发场景下 off 是开抢之前读的，数字已经过时，重读一次再报给用户
            CourseOffering latest = offeringMapper.selectById(offeringId);
            int enrolled = latest != null && latest.getEnrolled() != null ? latest.getEnrolled() : 0;
            int capacity = latest != null && latest.getCapacity() != null ? latest.getCapacity() : 0;
            throw new BizException(409, "该课程名额已满（" + enrolled + "/" + capacity + "）");
        }

        if (exist != null) {
            // 曾经退过课：复用原记录，避免唯一索引冲突
            exist.setStatus(1);
            exist.setCreatedAt(LocalDateTime.now());
            enrollmentMapper.updateById(exist);
            return exist.getId();
        }
        Enrollment e = new Enrollment();
        e.setOfferingId(offeringId);
        e.setStudentId(studentId);
        e.setStatus(1);
        enrollmentMapper.insert(e);
        return e.getId();
    }

    @Transactional
    public void drop(Long enrollmentId, Long studentId) {
        Enrollment e = enrollmentMapper.selectById(enrollmentId);
        if (e == null) {
            throw new BizException(404, "选课记录不存在");
        }
        if (!e.getStudentId().equals(studentId)) {
            throw new BizException(403, "只能退自己选的课");
        }
        if (!Integer.valueOf(1).equals(e.getStatus())) {
            throw new BizException("该课程已退课，无需重复操作");
        }
        Long scored = scoreMapper.selectCount(
                Wrappers.<Score>lambdaQuery().eq(Score::getEnrollmentId, enrollmentId));
        if (scored != null && scored > 0) {
            throw new BizException("该课程已录入成绩，不能退课");
        }
        e.setStatus(0);
        enrollmentMapper.updateById(e);
        offeringMapper.releaseSeat(e.getOfferingId());
    }

    /**
     * 我的课表。不传学期时默认当前学期 —— 「我的课表」的业务语义就是本学期，
     * 查历史学期要显式传 termId，避免学生把上学期已结课的记录当成在读课程。
     */
    @Transactional(readOnly = true)
    public List<MyCourseVO> myCourses(Long studentId, Long termId, boolean onlyActive) {
        Long scope = termId != null ? termId : currentTermId();
        return enrollmentMapper.selectMyCourses(studentId, scope, onlyActive);
    }

    private Long currentTermId() {
        Term current = termMapper.selectOne(Wrappers.<Term>lambdaQuery()
                .eq(Term::getIsCurrent, 1).last("LIMIT 1"));
        return current == null ? null : current.getId();
    }
}
