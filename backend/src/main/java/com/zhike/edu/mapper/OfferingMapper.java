package com.zhike.edu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhike.edu.dto.OfferingVO;
import com.zhike.edu.entity.CourseOffering;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface OfferingMapper extends BaseMapper<CourseOffering> {

    @Select("""
            <script>
            SELECT o.id, o.term_id, t.name AS term_name, o.course_id, c.code AS course_code,
                   c.name AS course_name, c.credit, c.category,
                   o.teacher_id, u.real_name AS teacher_name, cl.name AS clazz_name,
                   o.weekday, o.start_section, o.end_section, o.location,
                   o.capacity, o.enrolled, o.status
              FROM edu_offering o
              JOIN edu_course c ON c.id = o.course_id
              JOIN edu_term t ON t.id = o.term_id
              JOIN sys_user u ON u.id = o.teacher_id
              LEFT JOIN edu_clazz cl ON cl.id = o.clazz_id
             <where>
               <if test="termId != null"> AND o.term_id = #{termId} </if>
               <if test="teacherId != null"> AND o.teacher_id = #{teacherId} </if>
               <if test="keyword != null and keyword != ''">
                 AND (c.name LIKE CONCAT('%', #{keyword}, '%') OR c.code LIKE CONCAT('%', #{keyword}, '%'))
               </if>
             </where>
             ORDER BY o.weekday, o.start_section, c.code
            </script>
            """)
    List<OfferingVO> selectVoList(@Param("termId") Long termId,
                                  @Param("teacherId") Long teacherId,
                                  @Param("keyword") String keyword);

    /**
     * 原子占座：一条 UPDATE 同时完成「判断是否还有名额」和「名额 +1」。
     * 高并发下靠 InnoDB 行锁保证不会超卖，返回 0 表示已满 —— 不需要先查后改，也就没有检查与写入之间的竞态窗口。
     */
    @Update("UPDATE edu_offering SET enrolled = enrolled + 1 WHERE id = #{id} AND enrolled < capacity AND status = 1")
    int tryOccupy(@Param("id") Long id);

    /** 退课释放名额，保证 enrolled 不会被减成负数 */
    @Update("UPDATE edu_offering SET enrolled = enrolled - 1 WHERE id = #{id} AND enrolled > 0")
    int releaseSeat(@Param("id") Long id);
}
