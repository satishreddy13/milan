package io.milan.connector.impl;

import io.milan.connector.ConfigField;
import io.milan.connector.ConnectorDescriptor;
import io.milan.connector.ConnectorHandler;
import io.milan.flow.FlowNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SOURCE — polls a local directory and processes each incoming file as a flow execution.
 * After processing, files are moved to a {@code .done} sub-directory (or deleted if configured).
 */
@Component
public class FileReaderConnector implements ConnectorHandler {

    @Override public String  getType()  { return "FILE_READER"; }
    @Override public boolean isSource() { return true; }

    @Override
    public ConnectorDescriptor describe() {
        return new ConnectorDescriptor(
                "FILE_READER",
                "File Reader",
                "TRIGGER",
                "Polls a directory and triggers the flow for each new file",
                List.of(
                        ConfigField.text  ("directory", "Directory",        true,  "/tmp/milan-input"),
                        ConfigField.text  ("pattern",   "File Pattern",     false, ".*\\.txt"),
                        ConfigField.select("after",     "After Processing", true,  "move",
                                          "move", "delete")
                )
        );
    }
}
