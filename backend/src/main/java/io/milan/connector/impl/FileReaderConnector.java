package io.milan.connector.impl;

import io.milan.connector.ConfigField;
import io.milan.connector.ConnectorDescriptor;
import io.milan.connector.ConnectorHandler;
import io.milan.flow.FlowNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SOURCE — polls a local directory and processes each file as a flow execution.
 *
 * After processing the file can be:
 *   move   → archived to archiveDir (default: .done sub-directory)
 *   delete → deleted immediately
 *   none   → left in place (reprocessed on next poll — use only for testing)
 *
 * Optional CSV parser: when parser=csv the body is parsed into a JSON array of objects.
 * The first row is used as headers when hasHeader=true.
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
                        ConfigField.text  ("directory",  "Directory",            true,  "/tmp/milan-input"),
                        ConfigField.text  ("pattern",    "File Pattern (regex)", false, ".*"),
                        ConfigField.select("after",      "After Processing",     true,  "move",
                                          "move", "delete", "none"),
                        ConfigField.text  ("archiveDir", "Archive Directory",    false, ".done"),
                        ConfigField.select("charset",    "Charset",              false, "UTF-8",
                                          "UTF-8", "UTF-16", "ISO-8859-1"),
                        ConfigField.select("parser",     "Parse As",             false, "none",
                                          "none", "csv"),
                        ConfigField.select("hasHeader",  "CSV Has Header Row",   false, "true",
                                          "true", "false")
                )
        );
    }
}
