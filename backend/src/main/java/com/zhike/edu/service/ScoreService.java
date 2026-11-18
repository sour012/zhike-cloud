package com.zhike.edu.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhike.common.BizException;
import com.zhike.edu.dto.RosterRow;
import com.zhike.edu.dto.ScoreUpsertRequest;
import com.zhike.edu.dto.ScoreVO;
import com.zhike.edu.entity.CourseOffering;
import com.zhike.edu.entity.Enrollment;
import com.zhike.edu.entity.Score;
import com.zhike.edu.mapper.EnrollmentMapper;
import com.zhike.edu.mapper.OfferingMapper;
import com.zhike.edu.mapper.ScoreMapper;
import com.zhike.security.LoginUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoreService {

    /** 平时分权重 30%，考试分 70%（与教务公示一致，改这里一处即可） */
    private static final BigDecimal USUAL_WEIGHT = new BigDecimal("0.3");
    private static final BigDecimal EXAM_WEIGHT = new BigDecimal("0.7");

    private final ScoreMapper scoreMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final OfferingMapper offeringMapper;

    public ScoreService(ScoreMapper scoreMapper, EnrollmentMapper enrollmentMapper,
                        OfferingMapper offeringMapper) {
        this.scoreMapper = scoreMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.offeringMapper = offeringMapper;
    }

    /**
     * 按角色收窄查询范围：学生只能看自己，教师只能看自己开的课，管理员可看全部。
     * 不把范围校验交给前端传参，避免改个 query 就能越权。
     */
    @Transactional(readOnly = true)
    public List<ScoreVO> list(LoginUser me, Long termId, String keyword) {
        Long studentId = me.isStudent() ? me.getId() : null;
        Long teacherId = me.isTeacher() && !me.isAdmin() ? me.getId() : null;
        return scoreMapper.selectVoList(studentId, teacherId, termId, keyword);
    }

    /** 教师查看某次开课的学生名单；管理员可看任意班，教师只能看自己的班 */
    @Transactional(readOnly = true)
    public List<RosterRow> roster(LoginUser me, Long offeringId) {
        CourseOffering off = offeringMapper.selectById(offeringId);
        if (off == null) {
            throw new BizException(404, "开课记录不存在");
        }
        if (!me.isAdmin() && !off.getTeacherId().equals(me.getId())) {
            throw new BizException(403, "只能查看自己任课班级的名单");
        }
        return scoreMapper.selectRoster(offeringId);
    }

    @Transactional
    public void save(LoginUser me, ScoreUpsertRequest req) {
        Enrollment e = enrollmentMapper.selectById(req.getEnrollmentId());
        if (e == null) {
            throw new BizException(404, "选课记录不存在");
        }
        if (!Integer.valueOf(1).equals(e.getStatus())) {
            throw new BizException("该学生已退课，不能录入成绩");
        }
        CourseOffering off = offeringMapper.selectById(e.getOfferingId());
        if (off == null) {
            throw new BizException(404, "开课记录不存在");
        }
        if (!me.isAdmin() && !off.getTeacherId().equals(me.getId())) {
            throw new BizException(403, "只能录入自己任课班级的成绩");
        }

        BigDecimal total = req.getUsualScore().multiply(USUAL_WEIGHT)
                .add(req.getExamScore().multiply(EXAM_WEIGHT))
                .setScale(1, RoundingMode.HALF_UP);

        Score score = scoreMapper.selectOne(Wrappers.<Score>lambdaQuery()
                .eq(Score::getEnrollmentId, req.getEnrollmentId()));
        if (score == null) {
            score = new Score();
            score.setEnrollmentId(req.getEnrollmentId());
            score.setStudentId(e.getStudentId());
            score.setOfferingId(e.getOfferingId());
        }
        score.setUsualScore(req.getUsualScore());
        score.setExamScore(req.getExamScore());
        score.setTotal(total);
        score.setGpa(toGpa(total));
        score.setUpdatedAt(LocalDateTime.now());
        if (score.getId() == null) {
            scoreMapper.insert(score);
        } else {
            scoreMapper.updateById(score);
        }
    }

    /** 学校 4.0 制换算表 */
    public static BigDecimal toGpa(BigDecimal total) {
        double v = total.doubleValue();
        double gpa;
        if (v >= 90) {
            gpa = 4.0;
        } else if (v >= 85) {
            gpa = 3.7;
        } else if (v >= 82) {
            gpa = 3.3;
        } else if (v >= 78) {
            gpa = 3.0;
        } else if (v >= 75) {
            gpa = 2.7;
        } else if (v >= 72) {
            gpa = 2.3;
        } else if (v >= 68) {
            gpa = 2.0;
        } else if (v >= 64) {
            gpa = 1.5;
        } else if (v >= 60) {
            gpa = 1.0;
        } else {
            gpa = 0.0;
        }
        return BigDecimal.valueOf(gpa).setScale(2, RoundingMode.HALF_UP);
    }

    /** 加权平均绩点与已修学分 */
    @Transactional(readOnly = true)
    public Summary summary(Long studentId) {
        BigDecimal weighted = scoreMapper.sumWeightedGpa(studentId);
        BigDecimal credits = scoreMapper.sumScoredCredits(studentId);
        if (credits == null || credits.signum() == 0) {
            return new Summary(BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }
        BigDecimal gpa = weighted.divide(credits, 2, RoundingMode.HALF_UP);
        Long count = scoreMapper.selectCount(Wrappers.<Score>lambdaQuery()
                .eq(Score::getStudentId, studentId).isNotNull(Score::getGpa));
        return new Summary(gpa, credits, count == null ? 0 : count.intValue());
    }

    public record Summary(BigDecimal gpa, BigDecimal credits, int courseCount) {
    }
}
