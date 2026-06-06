package io.milan.flow;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "flows")
public class Flow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlowStatus status = FlowStatus.DRAFT;

    /** Raw JSON stored as TEXT. Deserialised to FlowDefinition by FlowService. */
    @Column(columnDefinition = "text", nullable = false)
    private String definition = "{\"nodes\":[],\"edges\":[]}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---- accessors ----
    public UUID          getId()          { return id; }
    public String        getName()        { return name; }
    public String        getDescription() { return description; }
    public FlowStatus    getStatus()      { return status; }
    public String        getDefinition()  { return definition; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public LocalDateTime getUpdatedAt()   { return updatedAt; }

    public void setName(String name)               { this.name        = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(FlowStatus status)       { this.status      = status; }
    public void setDefinition(String definition)   { this.definition  = definition; }
}
