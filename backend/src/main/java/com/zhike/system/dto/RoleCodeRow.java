package com.zhike.system.dto;

import lombok.Data;

/** 用户-角色 关联行，用于批量装配用户列表（避免逐行查角色）。 */
@Data
public class RoleCodeRow {
    private Long userId;
    private String code;
}
