package com.zhike.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long parentId;
    private String title;
    private String path;

    /** 权限点，如 edu:course:add；目录与按钮以外的菜单行也带 perm 供前端控制显隐 */
    private String perm;

    /** 1 目录 2 菜单 3 按钮 */
    private Integer menuType;

    private String icon;
    private Integer sortNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
