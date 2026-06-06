package io.milan.engine;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.camel.ProducerTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Single static Spring MVC endpoint that handles all /trigger/** requests.
 * Looks up the active flow for (method, path) and dispatches to its Camel direct: route.
 */
@RestController
public class TriggerController {

    private final TriggerRegistry  registry;
    private final ProducerTemplate producerTemplate;

    public TriggerController(TriggerRegistry registry, ProducerTemplate producerTemplate) {
        this.registry         = registry;
        this.producerTemplate = producerTemplate;
    }

    @RequestMapping("/trigger/**")
    public ResponseEntity<String> dispatch(HttpServletRequest request) throws IOException {
        String fullPath = request.getRequestURI();
        // Strip "/trigger" prefix → e.g. "/webhook"
        String path    = fullPath.replaceFirst("^/trigger", "");
        String method  = request.getMethod();
        String body    = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

        UUID flowId = registry.resolve(method, path).orElse(null);
        if (flowId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No active flow listening on " + method + " " + path);
        }

        String directUri = "direct:flow-" + flowId;
        Object result = producerTemplate.requestBody(directUri, body);
        return ResponseEntity.ok(result != null ? result.toString() : "");
    }
}
