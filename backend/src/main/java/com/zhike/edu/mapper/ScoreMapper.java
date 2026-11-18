package com.zhike.edu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhike.edu.dto.RosterRow;
import com.zhike.edu.dto.ScoreVO;
import com.zhike.edu.entity.Score;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ScoreMapper extends BaseMapper<Score> {

    @Select("""
            <script>
            SELECT s.id, s.enrollment_id, s.student_id,
                   stu.username AS student_no, stu.real_name AS student_name,
                   c.code AS course_code, c.name AS course_name, c.credit,
                   t.name AS term_name,
                   s.usual_score, s.exam_score, s.total, s.gpa
              FROM edu_score s
              JOIN sys_user stu ON stu.id = s.student_id
              JOIN edu_offering o ON o.id = s.offering_id
              JOIN edu_course c ON c.id = o.course_id
              JOIN edu_term t ON t.id = o.term_id
             <where>
               <if test="studentId != null"> AND s.student_id = #{studentId} </if>
               <if test="teacherId != null"> AND o.teacher_id = #{teacherId} </if>
               <if test="termId != null"> AND o.term_id = #{termId} </if>
               <if test="keyword != null and keyword != ''">
                 AND (stu.real_name LIKE CONCAT('%', #{keyword}, '%')
                      OR stu.username LIKE CONCAT('%', #{keyword}, '%')
                      OR c.name LIKE CONCAT('%', #{keyword}, '%'))
               </if>
             </where>
             ORDER BY t.id DESC, c.code, s.total DESC
            </script>
            """)
    List<ScoreVO> selectVoList(@Param("studentId") Long studentId,
                               @Param("teacherId") Long teacherId,
                               @Param("termId") Long termId,
                               @Param("keyword") String keyword);

    /** 某次开课的选课名单（用于教师录入成绩） */
    @Select("""
            SELECT e.id AS enrollment_id, e.student_id,
                   stu.username AS student_no, stu.real_name AS student_name,
                   cl.name AS clazz_name,
                   s.usual_score, s.exam_score, s.total, s.gpa
              FROM edu_enrollment e
              JOIN sys_user stu ON stu.id = e.student_id
              LEFT JOIN edu_clazz cl ON cl.id = (SELECT clazz_id FROM edu_offering WHERE id = e.offering_id)
              LEFT JOIN edu_score s ON s.enrollment_id = e.id
             WHERE e.offering_id = #{offeringId} AND e.status = 1
             ORDER BY stu.username
            """)
    List<RosterRow> selectRoster(@Param("offeringId") Long offeringId);

    /** 加权 GPA = Σ(绩点 × 学分) / Σ学分，只算已出成绩的课程 */
    @Select("""
            SELECT COALESCE(SUM(s.gpa * c.credit), 0)
              FROM edu_score s
              JOIN edu_offering o ON o.id = s.offering_id
              JOIN edu_course c ON c.id = o.course_id
             WHERE s.student_id = #{studentId} AND s.gpa IS NOT NULL
            """)
    BigDecimal sumWeightedGpa(@Param("studentId") Long studentId);

    @Select("""
            SELECT COALESCE(SUM(c.credit), 0)
              FROM edu_score s
              JOIN edu_offering o ON o.id = s.offering_id
              JOIN edu_course c ON c.id = o.course_id
             WHERE s.student_id = #{studentId} AND s.gpa IS NOT NULL
            """)
    BigDecimal sumScoredCredits(@Param("studentId") Long studentId);
}
