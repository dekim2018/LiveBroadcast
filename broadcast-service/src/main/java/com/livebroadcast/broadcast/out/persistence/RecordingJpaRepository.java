package com.livebroadcast.broadcast.out.persistence;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.livebroadcast.broadcast.domain.RecordingStatus;

public interface RecordingJpaRepository extends JpaRepository<RecordingJpaEntity, Long> {

    Optional<RecordingJpaEntity> findFirstByMediaPathAndStatusOrderByIdDesc(String mediaPath, RecordingStatus status);

    Page<RecordingJpaEntity> findByStatusIn(Collection<RecordingStatus> statuses, Pageable pageable);

    Page<RecordingJpaEntity> findByBroadcastIdAndStatusIn(Long broadcastId, Collection<RecordingStatus> statuses,
                                                          Pageable pageable);

    boolean existsByMediaPathAndStatus(String mediaPath, RecordingStatus status);
}
