package io.milan.flow;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/flows")
public class FlowController {

    private final FlowService flowService;

    public FlowController(FlowService flowService) {
        this.flowService = flowService;
    }

    @GetMapping
    public List<FlowDto> list() {
        return flowService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FlowDto create(@Valid @RequestBody CreateFlowRequest req) throws Exception {
        return flowService.create(req);
    }

    @GetMapping("/{id}")
    public FlowDto get(@PathVariable UUID id) {
        return flowService.getById(id);
    }

    @PutMapping("/{id}")
    public FlowDto update(@PathVariable UUID id, @RequestBody UpdateFlowRequest req) throws Exception {
        return flowService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) throws Exception {
        flowService.delete(id);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<String> start(@PathVariable UUID id) {
        try {
            flowService.start(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<Void> stop(@PathVariable UUID id) {
        try {
            flowService.stop(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/trigger")
    public ResponseEntity<String> trigger(@PathVariable UUID id,
                                          @RequestBody(required = false) String body) {
        try {
            String result = flowService.trigger(id, body);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
