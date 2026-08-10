package com.mr.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BaseTimeDeletedEntity extends BaseTimeEntity {

    @Column(name = "deleted_at")
    protected Instant deletedAt;

    public void softDelete() {
        if (this.deletedAt == null) {
            this.deletedAt = Instant.now();
        }
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}