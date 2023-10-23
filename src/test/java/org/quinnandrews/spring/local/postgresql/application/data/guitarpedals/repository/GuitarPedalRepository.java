package org.quinnandrews.spring.local.postgresql.application.data.guitarpedals.repository;

import org.quinnandrews.spring.local.postgresql.application.data.guitarpedals.GuitarPedal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuitarPedalRepository extends JpaRepository<GuitarPedal, Long> {

    @Query(nativeQuery = true, value = "select usename from pg_user order by usename;")
    List<String> getPostgreSQLUsers();

    @Query(nativeQuery = true, value = "select lower(user_name) from information_schema.users order by user_name;")
    List<String> getH2Users();
}
