package com.zhike.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhike.system.entity.SysMenu;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /** 侧边栏用：该用户可见的目录与菜单（按钮不下发） */
    @Select("""
            SELECT DISTINCT m.id, m.parent_id, m.title, m.path, m.perm,
                   m.menu_type, m.icon, m.sort_no
              FROM sys_menu m
              JOIN sys_role_menu rm ON rm.menu_id = m.id
              JOIN sys_user_role ur ON ur.role_id = rm.role_id
             WHERE ur.user_id = #{userId}
               AND m.menu_type IN (1, 2)
             ORDER BY m.sort_no
            """)
    List<SysMenu> selectNavByUserId(@Param("userId") Long userId);

    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRole(@Param("roleId") Long roleId);
}
