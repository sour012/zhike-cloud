package com.zhike.system.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 侧边栏菜单树节点。 */
@Data
public class MenuNode {

    private Long id;
    private String title;
    private String path;
    private String perm;
    private String icon;
    private List<MenuNode> children = new ArrayList<>();

    public MenuNode(Long id, String title, String path, String perm, String icon) {
        this.id = id;
        this.title = title;
        this.path = path;
        this.perm = perm;
        this.icon = icon;
    }
}
