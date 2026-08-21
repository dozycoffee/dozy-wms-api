package com.dozycoffee.wms.global.common;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public abstract class SoftDeletableEntity extends BaseEntity {

    private LocalDateTime deletedAt;
    private String deletedBy;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    protected void softDelete(String actor) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = actor;
    }
}
