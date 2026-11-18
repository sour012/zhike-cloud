package com.zhike.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhike.system.dto.RoleCodeRow;
import com.zhike.system.entity.SysUser;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 该用户拥有的权限点（去重） */
    @Select("""
            SELECT DISTINCT m.perm
              FROM sys_menu m
              JOIN sys_role_menu rm ON rm.menu_id = m.id
              JOIN sys_user_role ur ON ur.role_id = rm.role_id
             WHERE ur.user_id = #{userId}
               AND m.perm IS NOT NULL AND m.perm <> ''
            """)
    List<String> selectPerms(@Param("userId") Long userId);

    /** 该用户拥有的角色编码 */
    @Select("""
            SELECT r.code
              FROM sys_role r
              JOIN sys_user_role ur ON ur.role_id = r.id
             WHERE ur.user_id = #{userId}
            """)
    List<String> selectRoleCodes(@Param("userId") Long userId);

    /** 批量取角色，避免列表页 N+1 查询 */
    @Select("""
            <script>
            SELECT ur.user_id AS user_id, r.code AS code
              FROM sys_user_role ur
              JOIN sys_role r ON r.id = ur.role_id
             WHERE ur.user_id IN
             <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<RoleCodeRow> selectRoleRows(@Param("ids") Collection<Long> ids);
}
