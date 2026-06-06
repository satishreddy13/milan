package io.milan.connector.impl;

import io.milan.connector.ConfigField;
import io.milan.connector.ConnectorDescriptor;
import io.milan.connector.ConnectorHandler;
import io.milan.flow.FlowNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SOURCE — fires a flow on a Quartz cron schedule.
 * Accepts standard 5-field Unix cron ("* /5 * * * *") or 6-field Quartz cron ("0 0/5 * * * ?").
 * FlowRouteBuilder auto-converts 5-field → 6-field by prepending a "0" seconds field.
 */
@Component
public class SchedulerConnector implements ConnectorHandler {

    @Override public String  getType()  { return "SCHEDULER"; }
    @Override public boolean isSource() { return true; }

    @Override
    public ConnectorDescriptor describe() {
        return new ConnectorDescriptor(
                "SCHEDULER",
                "Scheduler",
                "TRIGGER",
                "Fires the flow on a Quartz cron schedule",
                List.of(
                        ConfigField.cron("cron",        "Cron Expression", true,  "0/30 * * * * ?"),
                        ConfigField.text("description", "Description",     false, "Every 30 seconds")
                )
        );
    }
}
