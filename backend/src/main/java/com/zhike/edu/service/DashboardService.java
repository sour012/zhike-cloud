package com.zhike.edu.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhike.edu.dto.DashboardVO;
import com.zhike.edu.dto.OfferingVO;
import com.zhike.edu.entity.Course;
import com.zhike.edu.entity.CourseOffering;
import com.zhike.edu.entity.Enrollment;
import com.zhike.edu.entity.Term;
import com.zhike.edu.mapper.CourseMapper;
import com.zhike.edu.mapper.EnrollmentMapper;
import com.zhike.edu.mapper.OfferingMapper;
import com.zhike.notice.NoticeService;
import com.zhike.security.LoginUser;
import com.zhike.system.entity.SysUser;
import com.zhike.system.mapper.SysUserMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final String[] WEEK = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private final CourseMapper courseMapper;
    private final OfferingMapper offeringMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final SysUserMapper userMapper;
    private final OfferingService offeringService;
    private final ScoreService scoreService;
    private final NoticeService noticeService;

    public DashboardService(CourseMapper courseMapper, OfferingMapper offeringMapper,
                            EnrollmentMapper enrollmentMapper, SysUserMapper userMapper,
                            OfferingService offeringService, ScoreService scoreService,
                            NoticeService noticeService) {
        this.courseMapper = courseMapper;
        this.offeringMapper = offeringMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.userMapper = userMapper;
        this.offeringService = offeringService;
        this.scoreService = scoreService;
        this.noticeService = noticeService;
    }

    @Transactional(readOnly = true)
    public DashboardVO load(LoginUser me) {
        DashboardVO vo = new DashboardVO();
        Term term = offeringService.currentTerm();
        vo.setCurrentTermName(term == null ? "未设置当前学期" : term.getName());

        vo.setCourseCount(nz(courseMapper.selectCount(null)));
        vo.setStudentCount(nz(userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUserType, 3))));
        vo.setTeacherCount(nz(userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUserType, 2))));

        List<OfferingVO> offerings = term == null ? List.of()
                : offeringMapper.selectVoList(term.getId(), null, null);
        vo.setOfferingCount(offerings.size());

        int capacity = offerings.stream().mapToInt(o -> o.getCapacity() == null ? 0 : o.getCapacity()).sum();
        int enrolled = offerings.stream().mapToInt(o -> o.getEnrolled() == null ? 0 : o.getEnrolled()).sum();
        vo.setEnrollmentCount(enrolled);
        vo.setAvgCapacityUsage(capacity == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(enrolled * 100.0 / capacity).setScale(1, RoundingMode.HALF_UP));

        // 按星期的排课量
        Map<Integer, Long> byWeek = new LinkedHashMap<>();
        for (OfferingVO o : offerings) {
            Integer w = o.getWeekday();
            if (w != null && w >= 1 && w <= 7) {
                byWeek.merge(w, 1L, Long::sum);
            }
        }
        List<DashboardVO.WeekLoad> weekLoad = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            weekLoad.add(new DashboardVO.WeekLoad(i, WEEK[i], byWeek.getOrDefault(i, 0L)));
        }
        vo.setWeekLoad(weekLoad);

        // 选课最热的 5 门
        List<OfferingVO> sorted = new ArrayList<>(offerings);
        sorted.sort(Comparator.comparingInt((OfferingVO o) -> o.getEnrolled() == null ? 0 : o.getEnrolled())
                .reversed());
        vo.setHotCourses(sorted.stream().limit(5)
                .map(o -> new DashboardVO.HotCourse(o.getCourseName(), o.getTeacherName(),
                        o.getEnrolled() == null ? 0 : o.getEnrolled(),
                        o.getCapacity() == null ? 0 : o.getCapacity()))
                .toList());

        vo.setNotices(noticeService.list(null).stream().limit(3).toList());

        if (me.isStudent()) {
            // 只统计当前学期：先取本学期开设的教学班 id，再从选课记录里筛
            List<Long> termOfferingIds = offerings.stream().map(OfferingVO::getId).toList();
            List<Enrollment> mine = termOfferingIds.isEmpty() ? List.of()
                    : enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                            .eq(Enrollment::getStudentId, me.getId())
                            .eq(Enrollment::getStatus, 1)
                            .in(Enrollment::getOfferingId, termOfferingIds));
            vo.setMyCourseCount((long) mine.size());
            BigDecimal credits = BigDecimal.ZERO;
            if (!mine.isEmpty()) {
                List<Long> offeringIds = mine.stream().map(Enrollment::getOfferingId).toList();
                for (CourseOffering off : offeringMapper.selectBatchIds(offeringIds)) {
                    Course c = courseMapper.selectById(off.getCourseId());
                    if (c != null && c.getCredit() != null) {
                        credits = credits.add(c.getCredit());
                    }
                }
            }
            vo.setMyCredits(credits);
            vo.setMyGpa(scoreService.summary(me.getId()).gpa());
        } else if (me.isTeacher()) {
            // 教师视角只关心自己的教学任务量，不用再看全校汇总
            List<OfferingVO> mineOfferings = offerings.stream()
                    .filter(o -> me.getId().equals(o.getTeacherId()))
                    .toList();
            vo.setMyOfferingCount((long) mineOfferings.size());
            vo.setMyStudentCount(mineOfferings.stream()
                    .mapToLong(o -> o.getEnrolled() == null ? 0 : o.getEnrolled())
                    .sum());
        }
        return vo;
    }

    private long nz(Long v) {
        return v == null ? 0 : v;
    }
}
