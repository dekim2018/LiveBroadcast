package com.livebroadcast.broadcast.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.livebroadcast.broadcast.domain.BroadcastStatus;

import jakarta.persistence.LockModeType;

public interface BroadcastJpaRepository extends JpaRepository<BroadcastJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BroadcastJpaEntity b where b.id = :id")
    Optional<BroadcastJpaEntity> findByIdForUpdate(@Param("id") Long id);

    Page<BroadcastJpaEntity> findByStatus(BroadcastStatus status, Pageable pageable);

    Page<BroadcastJpaEntity> findByOwnerId(Long ownerId, Pageable pageable);

    @Query("select b.id from BroadcastJpaEntity b where b.status = :status and b.streamOnline = false")
    List<Long> findIdsByStatusAndOffline(@Param("status") BroadcastStatus status);
}
