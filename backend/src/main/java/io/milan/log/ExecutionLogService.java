package io.milan.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ExecutionLogService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionLogService.class);

    private final ExecutionLogRepository repository;

    public ExecutionLogService(ExecutionLogRepository repository) {
        this.repository = repository;
    }

    /** Fire-and-forget log write — new transaction so Camel route errors don't roll it back. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID flowId, String nodeId, String level, String message) {
        try {
            repository.save(ExecutionLog.of(flowId, nodeId, level, message));
        } catch (Exception e) {
            log.warn("Failed to persist execution log for flow {}: {}", flowId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<ExecutionLog> getLatest(UUID flowId, int limit) {
        return repository.findByFlowIdOrderByCreatedAtDesc(flowId, PageRequest.of(0, limit));
    }
}
