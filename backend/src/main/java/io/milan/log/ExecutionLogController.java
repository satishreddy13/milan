package io.milan.log;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/flows/{flowId}/logs")
public class ExecutionLogController {

    private final ExecutionLogService logService;

    public ExecutionLogController(ExecutionLogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public List<ExecutionLog> get(
            @PathVariable UUID flowId,
            @RequestParam(defaultValue = "100") int limit) {
        return logService.getLatest(flowId, limit);
    }
}
