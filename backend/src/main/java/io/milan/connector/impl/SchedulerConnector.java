package io.milan.connector.impl;

import io.milan.connector.ConfigField;
import io.milan.connector.ConnectorDescriptor;
import io.milan.connector.ConnectorHandler;
import io.milan.flow.FlowNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SOURCE — fires a flow on a Quartz cron schedule.
 * The cron expression uses Quartz syntax (7 fields, seconds-first):
 *   "0/10 * * * * ?" = every 10 seconds
 *   "0 0/5 * * * ?"  = every 5 minutes
 *   "0 0 8 * * ?"    = every day at 08:00
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
                        ConfigField.text("cron",        "Cron Expression", true,  "0/30 * * * * ?"),
                        ConfigField.text("description", "Description",     false, "Every 30 seconds")
                )
        );
    }
}
