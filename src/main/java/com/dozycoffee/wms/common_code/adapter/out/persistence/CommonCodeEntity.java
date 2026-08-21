package com.dozycoffee.wms.common_code.adapter.out.persistence;

import com.dozycoffee.wms.common_code.domain.model.CommonCode;
import com.dozycoffee.wms.global.common.BaseEntity;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("common_code")
@Getter
public class CommonCodeEntity extends BaseEntity {

    @Id
    private String code;
    private String groupCode;
    private String name;
    private int sortOrder;
    private boolean active;

    private CommonCodeEntity() {}

    public CommonCode toDomain() {
        return CommonCode.reconstruct(code, groupCode, name, sortOrder, active);
    }

    public static CommonCodeEntity from(CommonCode domain) {
        CommonCodeEntity entity = new CommonCodeEntity();
        entity.code = domain.getCode();
        entity.groupCode = domain.getGroupCode();
        entity.name = domain.getName();
        entity.sortOrder = domain.getSortOrder();
        entity.active = domain.isActive();
        return entity;
    }
}
