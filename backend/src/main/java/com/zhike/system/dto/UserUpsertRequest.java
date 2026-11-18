package com.zhike.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/** 新增/编辑用户。新增时 password 必填，编辑时留空表示不改密码。 */
@Data
public class UserUpsertRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "长度需在 3~50 之间")
    private String username;

    @Size(min = 6, max = 32, message = "密码长度需在 6~32 之间")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    @NotNull(message = "用户类型不能为空")
    private Integer userType;

    private String dept;
    private String phone;
    private String email;

    private Integer status = 1;

    /** 角色编码，如 ["teacher"] */
    private List<String> roleCodes;
}
