package com.example.demo.repository;

import com.example.demo.entity.SystemInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemInfoRepository extends JpaRepository<SystemInfo, Long> {
    Optional<SystemInfo> findFirstByOrderByIdAsc();
}