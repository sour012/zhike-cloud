package com.zhike.edu.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhike.common.BizException;
import com.zhike.edu.TimeSlot;
import com.zhike.edu.dto.EnrolledSlot;
import com.zhike.edu.dto.OfferingVO;
import com.zhike.edu.entity.Clazz;
import com.zhike.edu.entity.CourseOffering;
import com.zhike.edu.entity.Enrollment;
import com.zhike.edu.entity.Term;
import com.zhike.edu.mapper.ClazzMapper;
import com.zhike.edu.mapper.EnrollmentMapper;
import com.zhike.edu.mapper.OfferingMapper;
import com.zhike.edu.mapper.TermMapper;
import com.zhike.system.entity.SysUser;
import com.zhike.system.mapper.SysUserMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfferingService {

    private final OfferingMapper offeringMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final TermMapper termMapper;
    private final ClazzMapper clazzMapper;
    private final SysUserMapper userMapper;

    public OfferingService(OfferingMapper offeringMapper, EnrollmentMapper enrollmentMapper,
                           TermMapper termMapper, ClazzMapper clazzMapper, SysUserMapper userMapper) {
        this.offeringMapper = offeringMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.termMapper = termMapper;
        this.clazzMapper = clazzMapper;
        this.userMapper = userMapper;
    }

    /** 任课教师下拉选项。只暴露 id 与姓名，不下发手机号邮箱等敏感字段。 */
    @Transactional(readOnly = true)
    public List<TeacherOption> teachers() {
        return userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUserType, 2)
                        .eq(SysUser::getStatus, 1)
                        .orderByAsc(SysUser::getId))
                .stream()
                .map(u -> new TeacherOption(u.getId(), u.getRealName()))
                .toList();
    }

    public record TeacherOption(Long id, String realName) {
    }

    @Transactional(readOnly = true)
    public List<Term> terms() {
        return termMapper.selectList(Wrappers.<Term>lambdaQuery().orderByAsc(Term::getId));
    }

    @Transactional(readOnly = true)
    public Term currentTerm() {
        return termMapper.selectOne(Wrappers.<Term>lambdaQuery()
                .eq(Term::getIsCurrent, 1).last("LIMIT 1"));
    }

    @Transactional(readOnly = true)
    public List<Clazz> clazzes() {
        return clazzMapper.selectList(Wrappers.<Clazz>lambdaQuery().orderByAsc(Clazz::getId));
    }

    /**
     * 开课列表。传了 studentId 时顺带算出「是否已选」和「是否与已选课程冲突」，
     * 让前端可以直接把冲突课程的按钮置灰，不用再发一轮请求。
     */
    @Transactional(readOnly = true)
    public List<OfferingVO> list(Long termId, Long teacherId, String keyword, Long studentId) {
        List<OfferingVO> rows = offeringMapper.selectVoList(termId, teacherId, keyword);
        if (studentId == null) {
            return rows;
        }
        Set<Long> selectedIds = new HashSet<>();
        for (Enrollment e : enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getStudentId, studentId).eq(Enrollment::getStatus, 1))) {
            selectedIds.add(e.getOfferingId());
        }
        List<EnrolledSlot> slots = enrollmentMapper.selectSlots(studentId, termId);
        for (OfferingVO vo : rows) {
            vo.setSelected(selectedIds.contains(vo.getId()));
            if (Boolean.TRUE.equals(vo.getSelected())) {
                continue;
            }
            for (EnrolledSlot s : slots) {
                if (TimeSlot.overlap(vo.getWeekday(), vo.getStartSection(), vo.getEndSection(),
                        s.getWeekday(), s.getStartSection(), s.getEndSection())) {
                    vo.setConflict(true);
                    vo.setConflictWith(s.getCourseName());
                    break;
                }
            }
        }
        return rows;
    }

    @Transactional
    public Long create(CourseOffering off) {
        validate(off);
        off.setId(null);
        off.setEnrolled(0);
        if (off.getStatus() == null) {
            off.setStatus(1);
        }
        offeringMapper.insert(off);
        return off.getId();
    }

    @Transactional
    public void update(Long id, CourseOffering off) {
        CourseOffering db = offeringMapper.selectById(id);
        if (db == null) {
            throw new BizException(404, "开课记录不存在");
        }
        validate(off);
        if (off.getCapacity() != null && off.getCapacity() < db.getEnrolled()) {
            throw new BizException("容量不能小于已选人数 " + db.getEnrolled());
        }
        off.setId(id);
        // 已选人数由选课流程维护，这里不允许手工覆盖
        off.setEnrolled(db.getEnrolled());
        offeringMapper.updateById(off);
    }

    @Transactional
    public void delete(Long id) {
        Long used = enrollmentMapper.selectCount(
                Wrappers.<Enrollment>lambdaQuery().eq(Enrollment::getOfferingId, id));
        if (used != null && used > 0) {
            throw new BizException("已有学生选课，不能删除；如需停开请把状态改为「关闭」");
        }
        offeringMapper.deleteById(id);
    }

    private void validate(CourseOffering off) {
        if (off.getTermId() == null || off.getCourseId() == null || off.getTeacherId() == null) {
            throw new BizException("学期、课程、任课教师不能为空");
        }
        if (off.getWeekday() == null || off.getWeekday() < 1 || off.getWeekday() > 7) {
            throw new BizException("上课星期需在 1~7 之间（1 为周一）");
        }
        Integer s = off.getStartSection();
        Integer e = off.getEndSection();
        if (s == null || e == null || s < 1 || e > 12 || s > e) {
            throw new BizException("节次不合法：起始节不得大于结束节，范围 1~12");
        }
        if (off.getCapacity() == null || off.getCapacity() <= 0) {
            throw new BizException("容量必须大于 0");
        }
    }
}
