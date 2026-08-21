package com.dozycoffee.wms.common_code.domain.model;

import lombok.Getter;

@Getter
public class CommonCode {

    private final String code;
    private final String groupCode;
    private final String name;
    private final int sortOrder;
    private final boolean active;

    private CommonCode(String code, String groupCode, String name, int sortOrder, boolean active) {
        this.code = code;
        this.groupCode = groupCode;
        this.name = name;
        this.sortOrder = sortOrder;
        this.active = active;
    }

    public static CommonCode create(String code, String groupCode, String name, int sortOrder) {
        return new CommonCode(code, groupCode, name, sortOrder, true);
    }

    public static CommonCode reconstruct(String code, String groupCode, String name, int sortOrder, boolean active) {
        return new CommonCode(code, groupCode, name, sortOrder, active);
    }
}
