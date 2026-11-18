package com.zhike.edu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhike.edu.dto.EnrolledSlot;
import com.zhike.edu.dto.MyCourseVO;
import com.zhike.edu.entity.Enrollment;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface EnrollmentMapper extends BaseMapper<Enrollment> {

    @Select("""
            <script>
            SELECT e.id AS enrollment_id, e.offering_id, c.code AS course_code, c.name AS course_name,
                   c.credit, c.category, u.real_name AS teacher_name,
                   o.weekday, o.start_section, o.end_section, o.location, e.status,
                   s.total, s.gpa
              FROM edu_enrollment e
              JOIN edu_offering o ON o.id = e.offering_id
              JOIN edu_course c ON c.id = o.course_id
              JOIN sys_user u ON u.id = o.teacher_id
              LEFT JOIN edu_score s ON s.enrollment_id = e.id
             WHERE e.student_id = #{studentId}
             <if test="termId != null"> AND o.term_id = #{termId} </if>
             <if test="onlyActive"> AND e.status = 1 </if>
             ORDER BY e.status DESC, o.weekday, o.start_section
            </script>
            """)
    List<MyCourseVO> selectMyCourses(@Param("studentId") Long studentId,
                                     @Param("termId") Long termId,
                                     @Param("onlyActive") boolean onlyActive);

    /** 该学生本学期已占用的时段（只算未退课的） */
    @Select("""
            <script>
            SELECT e.offering_id, c.name AS course_name, o.weekday, o.start_section, o.end_section
              FROM edu_enrollment e
              JOIN edu_offering o ON o.id = e.offering_id
              JOIN edu_course c ON c.id = o.course_id
             WHERE e.student_id = #{studentId} AND e.status = 1
             <if test="termId != null"> AND o.term_id = #{termId} </if>
            </script>
            """)
    List<EnrolledSlot> selectSlots(@Param("studentId") Long studentId,
                                   @Param("termId") Long termId);
}
