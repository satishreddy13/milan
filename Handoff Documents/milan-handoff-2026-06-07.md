# Milan Integration Platform — Handoff Document
**Date:** 2026-06-07  
**Repository:** https://github.com/satishreddy13/milan  
**Branch:** `main` (single branch, all work committed here)

---

## Goal

Build **Milan** — an open-source, MuleSoft-inspired visual integration platform where users design data flows by dragging connectors onto a canvas, connecting them, and running them. The system executes flows using Apache Camel under the hood, hiding all Camel complexity behind a clean drag-and-drop UI.

Core principles:
- No code required to build integrations
- Every connector type is a Spring `@Component` — adding new ones is self-contained
- The canvas saves/loads as a JSON definition (nodes + edges) stored in PostgreSQL
- The UI reflects live flow status and streams execution logs in real time

---

## Current Progress

### Phase 1 — Scaffold (complete)
- Spring Boot 3.3 + Apache Camel 4.8 backend on port `38080`
- React + Vite + React Flow frontend on port `5173`
- PostgreSQL on port `35432` (Docker)
- Flyway schema migration
- Full CRUD for flows (create, list, get, update, delete)
- Flow start/stop (Camel route lifecycle)
- Execution log table + streaming footer in the UI
- Connector palette with drag-and-drop onto canvas
- Node config panel (right sidebar, updates live)
- Save flow definition to DB; load on open

### Phase 2 — Connectors & UX polish (complete)

**Connectors added:**
- Manual HTTP trigger (⚡ Test button in editor toolbar)
- Scheduler (Quartz cron — accepts both 5-field Unix and 6-field Quartz formats)
- File Reader (poll directory, archive/delete, optional CSV parser)
- File Writer (text or CSV, configurable charset, append mode)
- CSV Parser (standalone transformer node)

**UX fixes:**
- Toast notification system replacing all `alert()` calls
- Flow list: rename modal (✏), delete confirmation modal, animated status pulse dot
- Flow editor: click flow name in header to rename inline
- Save button turns amber only on real user edits (excludes React Flow `dimensions`/`select` internal events)
- Node config panel reads live store state (fixes stale config display)
- Delete node button in config panel
- Live cron validation with human-readable description via `cronstrue`

### Phase 2.5 — Expression Language + Core EIPs (complete)

**Expression language:**
- New `expression` field type with monospace textarea
- Collapsible variable reference panel — click any variable to insert at cursor
- Groups: Message, File headers, HTTP headers, Date/Time, Operators

**EIP connectors:**
- **Filter** — drops exchanges where Simple condition is false
- **Splitter** — splits by line/csv/jsonArray/expression; streaming + parallel options
- **Set Header** — sets a message header from a Simple expression
- **Content-Based Router (Choice)** — two-branch routing (when/otherwise handles); yellow node with dual source handles

**Architecture change:** `ConnectorHandler.applyAndReturn()` default method added so structural connectors (Splitter, Filter, Choice, Aggregator) can return a child `ProcessorDefinition` — all subsequent nodes in the chain wire inside the structural block.

`FlowRouteBuilder` migrated from linear `buildNodeChain()` loop to **recursive graph traversal** (`buildRouteSegment()`), enabling arbitrary branching.

### Phase 3 — Error Handling, Wire Tap, Aggregator (complete)

**Error Handler** (🛡, standalone node — drop anywhere, no connections):
- Configures global `onException`: max retries, retry delay, dead-letter directory
- Detected by type before route building; excluded from graph traversal

**Try / Catch** (🔒, inline node):
- Wraps all subsequent chain nodes in Camel `doTry/doCatch`
- Actions: `log` (swallow + continue), `deadLetter` (write error + body to dir), `rethrow` (escalate to global handler)
- `TryFrame` record threaded through `buildRouteSegment()` recursion

**Wire Tap** (📡):
- Async fire-and-forget copy to log or file
- Main exchange continues immediately (uses Camel `wireTap()`)

**Aggregator** (🗃):
- Reassembles split sub-messages back into one
- Strategies: `collect` (→ JSON array), `concatenate` (with separator), `latest`
- Completion: by message count, timeout (ms), or both

---

## What Worked

### Architecture patterns

**Static `TriggerController` + `TriggerRegistry` for HTTP flows**  
Camel's `platform-http` component does not deregister Spring MVC mappings when a route is removed, causing "ambiguous mapping" errors on restart. Solved by routing all HTTP trigger traffic through a single static `@RequestMapping("/trigger/**")` controller that dispatches to `direct:flow-{id}` via a `ConcurrentHashMap` registry. All flows use `direct:` as their main entry point.

**Two-route pattern for non-HTTP sources**  
SCHEDULER and FILE_READER use a "feeder" route (quartz/file → direct:) plus a "main" route (direct: → processors). This lets the manual ⚡ Test button always trigger via `ProducerTemplate.requestBody("direct:flow-{id}", body)` regardless of source type.

**`applyAndReturn()` on `ConnectorHandler`**  
Adding a default method (not breaking existing `apply()` implementations) that returns `ProcessorDefinition<?>` lets structural connectors (Splitter, Filter, Choice, Aggregator, TryCatch) return a child definition. The recursive builder threads the current context correctly. Non-structural connectors inherit the default which calls `apply()` and returns `route` unchanged.

**Recursive `buildRouteSegment()` with `TryFrame`**  
Replacing the linear list traversal with recursive graph traversal handles arbitrary topology: linear chains, Choice branching, Splitter sub-contexts, and TryCatch blocks. The `TryFrame` record (holding `TryDefinition` + config) is threaded through the recursion and closed at the end of the protected chain.

**Standalone `ERROR_HANDLER` node**  
A node type not in the main processing chain. `findSourceNode()` excludes it via `STANDALONE_TYPES` set. `configure()` scans all nodes for it before route building and uses its config to shape `onException`. Pattern can be reused for future flow-level config nodes.

**Zustand individual selectors**  
Object selectors (`useFlowStore(s => ({ a: s.a, b: s.b }))`) create new references every render, causing infinite re-render loops in React Flow. Always use individual selectors (`useFlowStore(s => s.field)`). This was the root cause of the blank-page crash on node click.

**`FlowEdge.sourceHandle` for multi-output nodes**  
Adding a nullable `sourceHandle` field to `FlowEdge` (both Java record and TypeScript type) allows the Choice node's `when`/`otherwise` edges to be distinguished in the graph traversal. Preserved on save/load in `FlowEditorPage`.

**5-field → 6-field cron auto-conversion**  
Quartz requires 6 fields (seconds-first); Unix cron has 5. Auto-detect by splitting on whitespace and prepending `"0 "` if length is 5. Happens in `FlowRouteBuilder.configureSource()`. The frontend `cronstrue` library also normalises before parsing for the live preview.

**`onException` must come before `from()` in Camel RouteBuilder**  
Camel throws at startup if `onException` is declared after any `from()`. Always declare it first in `configure()`.

### UI patterns

**Expression field type with variable reference panel**  
The `ExpressionInput` component renders a monospace textarea with a collapsible "Variables reference" panel. Clicking any variable inserts it at the cursor using `element.setSelectionRange()` and `requestAnimationFrame` to restore position after React re-renders. Makes Camel Simple Language approachable without docs.

**`ExpressionInput.parseCron()` for live validation**  
Normalise to 6-field before passing to `cronstrue.toString({ throwExceptionOnParseError: true })`. Green `✓ Every 5 minutes` on valid, red border + `✗ Invalid cron expression` on invalid.

**React Flow `dimensions` and `select` change types**  
These fire during canvas initialisation and on click — not user edits. Excluding them from dirty-state detection (`onNodesChange`) keeps the save button clean on page load.

---

## What Didn't Work

**`route.log(LoggingLevel.INFO, ...)` for Logger connector**  
Emitted at ERROR level in SLF4J regardless of the configured level. Root cause unknown (likely a Camel platform-http interaction). Fixed by using `Logger.info()/warn()/error()` directly inside a `route.process()` lambda.

**`platform-http` component for dynamic HTTP listener routes**  
Routes added via `from("platform-http:/path")` do not clean up their Spring MVC handler mappings when the route is removed. Restarting a flow caused `IllegalStateException: Ambiguous handler methods mapped for '/path'`. Cannot be fixed without patching Camel internals. Replaced entirely with `TriggerController` + `TriggerRegistry`.

**`onException` after `from()` in RouteBuilder**  
Camel throws `IllegalArgumentException: onException must be defined before any routes`. Must always be the first call in `configure()`.

**`useFlowStore(s => ({ a: s.a, b: s.b }))` object selector**  
New object reference on every render → `NodeConfigPanel` re-renders infinitely → React crashes with blank page. Never use object selectors from Zustand; always destructure with individual selectors.

**`setNodes()` called on new node drop marking canvas dirty**  
`FlowCanvas.onDrop` called `setNodes([...nodes, newNode])` which went through `setNodes` (which cleared dirty). But dropping a node IS a user edit — dirty should be set. Fixed by checking change type in `onNodesChange` rather than relying on `setNodes` for dirty tracking.

**`TryDefinition.handled(boolean)`**  
`handled()` is a method on `OnExceptionDefinition`, not on `TryDefinition`. In `doTry/doCatch`, exceptions are always caught (like Java). For rethrow, must explicitly throw inside a `process()` lambda. Calling `handled()` on `TryDefinition` causes a compilation error.

**Standalone `TRY_CATCH` + `CHOICE` nesting**  
The `TryFrame` is not passed into `buildChoiceSegment()` branches — both branches receive `null` for `openTry`. Nesting a Choice inside a TryCatch means the try block never closes. Document as unsupported in the connector description.

---

## Next Steps

### Immediate (iteration 4)
- **Multi-condition Choice** — support 3+ `when` branches with dynamic handles (up to 5 conditions). Requires dynamic handle rendering in `ChoiceNode` and indexed `when-0`, `when-1` sourceHandles.
- **Flow validation before start** — check that: source node exists, all required config fields are filled, no disconnected nodes. Surface as a list of errors in the UI before hitting the backend.
- **Execution log improvements** — per-node log filtering, log level badges, auto-scroll with pause-on-hover.

### Near-term (iteration 5)
- **Transform connector** — JSON path extraction, field mapping, template rendering (Freemarker/Mustache via Camel)
- **JDBC / database connector** — execute SQL queries, use result as body; Camel SQL component
- **Email connector** — send email via SMTP; Camel mail component
- **Slack / webhook connector** — POST to an incoming webhook URL

### Architecture improvements
- **Flow versioning** — snapshot `definition` on each save, allow rollback
- **Multi-condition Choice convergence** — allow branches to rejoin at a common downstream node (requires edge-based graph merging, not just sourceHandle grouping)
- **Auth / multi-user** — Spring Security + JWT; per-user flow isolation
- **Deployment** — Docker Compose for the full stack; health endpoints; production-grade logging

---

## State

### Repository
```
GitHub:  https://github.com/satishreddy13/milan
Branch:  main
Latest:  c073f91  feat: error handling, wire tap, and aggregator (iteration 3)
```

### Runtime ports
| Service    | Port  | Notes                              |
|------------|-------|------------------------------------|
| Backend    | 38080 | Spring Boot JAR or `mvn spring-boot:run` |
| Frontend   | 5173  | `npm run dev` (Vite)               |
| PostgreSQL | 35432 | Docker; DB name/user/pass: `milan` |

### Key backend files
| File | Purpose |
|------|---------|
| `engine/FlowRouteBuilder.java` | Core: builds all Camel DSL from flow definition |
| `engine/FlowEngine.java` | Start/stop/trigger flow lifecycle |
| `engine/TriggerController.java` | Static HTTP dispatcher for all HTTP_LISTENER flows |
| `engine/TriggerRegistry.java` | Maps `METHOD:/path` → flowId |
| `connector/ConnectorHandler.java` | SPI interface; `applyAndReturn()` is the key method |
| `connector/ConfigField.java` | Factory methods: `.text()`, `.select()`, `.expression()`, `.cron()` |
| `flow/FlowRouteBuilder.java` | Graph traversal entry point |

### All connector types (backend `@Component` implementations)
| Type | Category | File |
|------|----------|------|
| `HTTP_LISTENER` | TRIGGER | `HttpListenerConnector.java` |
| `SCHEDULER` | TRIGGER | `SchedulerConnector.java` |
| `FILE_READER` | TRIGGER | `FileReaderConnector.java` |
| `HTTP_REQUEST` | ACTION | `HttpRequestConnector.java` |
| `FILE_WRITER` | ACTION | `FileWriterConnector.java` |
| `LOGGER` | PROCESSOR | `LoggerConnector.java` |
| `SET_BODY` | TRANSFORMER | `SetBodyConnector.java` |
| `SET_HEADER` | TRANSFORMER | `SetHeaderConnector.java` |
| `CSV_PARSER` | TRANSFORMER | `CsvParserConnector.java` |
| `FILTER` | PROCESSOR | `FilterConnector.java` |
| `SPLITTER` | PROCESSOR | `SplitterConnector.java` |
| `CHOICE` | PROCESSOR | `ChoiceConnector.java` |
| `WIRE_TAP` | PROCESSOR | `WireTapConnector.java` |
| `TRY_CATCH` | PROCESSOR | `TryCatchConnector.java` |
| `AGGREGATOR` | PROCESSOR | `AggregatorConnector.java` |
| `ERROR_HANDLER` | PROCESSOR | `ErrorHandlerConnector.java` *(standalone, no chain connections)* |

### Key frontend files
| File | Purpose |
|------|---------|
| `store/flowStore.ts` | Zustand store: nodes, edges, selectedNode, dirty flag |
| `pages/FlowEditorPage.tsx` | Main editor: toolbar, canvas, config panel, test modal |
| `pages/FlowListPage.tsx` | Flow list with rename/delete modals |
| `components/flow/FlowCanvas.tsx` | React Flow canvas; `nodeTypes` map must include every connector |
| `components/flow/NodeConfigPanel.tsx` | Right sidebar; renders all field types including `expression` and `cron` |
| `components/flow/ExpressionInput.tsx` | Expression textarea with variable reference panel |
| `context/ToastContext.tsx` | Global toast notification system |
| `types/flow.ts` | `FlowDefinition`, `MilanNode`, `MilanEdge` (includes `sourceHandle`) |
| `types/connector.ts` | `ConfigField` type (field types: `string`, `select`, `textarea`, `cron`, `expression`) |

### Database schema
```sql
-- flows: stores definition as TEXT (JSON)
-- execution_logs: per-exchange log entries, indexed by flow_id + created_at DESC
-- Managed by Flyway: V1__init.sql
```

### Dependencies worth noting
- **Apache Camel 4.8.0** — `camel-spring-boot-starter`, `camel-platform-http-starter`, `camel-http-starter`, `camel-quartz-starter`, `camel-csv-starter`
- **Frontend:** `@xyflow/react`, `zustand`, `cronstrue`, `react-router-dom`, `axios`
- **Spring Boot 3.3.5** with virtual threads (`spring.threads.virtual.enabled=true`)

### Adding a new connector (checklist)
1. Create `backend/.../connector/impl/XxxConnector.java` — implement `ConnectorHandler`, annotate `@Component`
2. Override `apply()` for action/processor connectors, `applyAndReturn()` for structural ones
3. Create `frontend/.../components/nodes/XxxNode.tsx` — use `BaseNode` or custom layout
4. Register in `FlowCanvas.tsx` `nodeTypes` map
5. Build both (`mvn package -DskipTests` + `npm run build`) and restart backend
