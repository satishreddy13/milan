package io.milan.flow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FlowRepository extends JpaRepository<Flow, UUID> {
    List<Flow> findByStatus(FlowStatus status);
    List<Flow> findAllByOrderByCreatedAtDesc();
}
