package io.milan.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.milan.engine.FlowEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FlowService {

    private final FlowRepository flowRepository;
    private final FlowEngine     flowEngine;
    private final ObjectMapper   objectMapper;

    public FlowService(FlowRepository flowRepository, FlowEngine flowEngine,
                       ObjectMapper objectMapper) {
        this.flowRepository = flowRepository;
        this.flowEngine     = flowEngine;
        this.objectMapper   = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<FlowDto> listAll() {
        return flowRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public FlowDto getById(UUID id) {
        return toDto(find(id));
    }

    @Transactional
    public FlowDto create(CreateFlowRequest req) throws Exception {
        Flow flow = new Flow();
        flow.setName(req.name());
        flow.setDescription(req.description());
        if (req.definition() != null) {
            flow.setDefinition(objectMapper.writeValueAsString(req.definition()));
        }
        return toDto(flowRepository.save(flow));
    }

    @Transactional
    public FlowDto update(UUID id, UpdateFlowRequest req) throws Exception {
        Flow flow = find(id);
        if (req.name()        != null) flow.setName(req.name());
        if (req.description() != null) flow.setDescription(req.description());
        if (req.definition()  != null) {
            flow.setDefinition(objectMapper.writeValueAsString(req.definition()));
        }
        return toDto(flowRepository.save(flow));
    }

    @Transactional
    public void delete(UUID id) throws Exception {
        if (flowEngine.isActive(id)) flowEngine.stopFlow(id);
        flowRepository.deleteById(id);
    }

    public void start(UUID id) throws Exception {
        flowEngine.startFlow(id);
    }

    public void stop(UUID id) throws Exception {
        flowEngine.stopFlow(id);
    }

    public String trigger(UUID id, String body) throws Exception {
        return flowEngine.triggerFlow(id, body);
    }

    // -----------------------------------------------------------------------

    private Flow find(UUID id) {
        return flowRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Flow not found: " + id));
    }

    private FlowDto toDto(Flow flow) {
        try {
            JsonNode def = objectMapper.readTree(flow.getDefinition());
            return new FlowDto(flow.getId(), flow.getName(), flow.getDescription(),
                    flow.getStatus(), def, flow.getCreatedAt(), flow.getUpdatedAt());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse flow definition for " + flow.getId(), e);
        }
    }
}
