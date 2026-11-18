package com.zhike.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhike.notice.Notice;
import com.zhike.notice.dto.NoticeRow;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface NoticeMapper extends BaseMapper<Notice> {

    @Select("""
            <script>
            SELECT n.id, n.title, n.content, n.publisher_id, u.real_name AS publisher_name, n.published_at
              FROM sys_notice n
              LEFT JOIN sys_user u ON u.id = n.publisher_id
             <where>
               <if test="keyword != null and keyword != ''">
                 AND n.title LIKE CONCAT('%', #{keyword}, '%')
               </if>
             </where>
             ORDER BY n.published_at DESC
            </script>
            """)
    List<NoticeRow> selectRows(@Param("keyword") String keyword);
}
