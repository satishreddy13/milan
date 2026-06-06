package io.milan.connector;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/connectors")
public class ConnectorController {

    private final ConnectorRegistry registry;

    public ConnectorController(ConnectorRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<ConnectorDescriptor> list() {
        return registry.allDescriptors();
    }
}
