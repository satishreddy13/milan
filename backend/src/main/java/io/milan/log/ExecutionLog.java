package io.milan.log;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "execution_logs")
public class ExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "flow_id", nullable = false)
    private UUID flowId;

    @Column(name = "node_id")
    private String nodeId;

    @Column(nullable = false)
    private String level = "INFO";

    @Column(columnDefinition = "text")
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static ExecutionLog of(UUID flowId, String nodeId, String level, String message) {
        ExecutionLog log = new ExecutionLog();
        log.flowId  = flowId;
        log.nodeId  = nodeId;
        log.level   = level;
        log.message = message;
        return log;
    }

    // ---- accessors ----
    public UUID          getId()        { return id; }
    public UUID          getFlowId()    { return flowId; }
    public String        getNodeId()    { return nodeId; }
    public String        getLevel()     { return level; }
    public String        getMessage()   { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
