package com.zhike.system.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private Integer userType;
    private String dept;
    private String phone;
    private String email;
    private Integer status;
    private List<String> roles;
    private LocalDateTime createdAt;
}
