package io.github.quinnandrews.spring.local.postgresql.application.data.guitarpedals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Table(name = "guitar_pedal")
@Entity
public class GuitarPedal {

    @Id
    @Column(name = "id",
            columnDefinition = "BIGINT",
            nullable = false,
            updatable = false)
    private Long id;

    @Column(name = "name",
            columnDefinition = "VARCHAR(63)",
            nullable = false)
    private String name;

    public GuitarPedal() {
        // no-op
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
