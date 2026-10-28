package edu.srmist.outpass.api;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Starts one hostel-outpass process instance per request; the body becomes the process variables. */
@RestController
public class OutpassController {

    static final String PROCESS_ID = "hostel-outpass";

    private final CamundaClient camundaClient;
    private final String defaultEscalateAfter;

    public OutpassController(CamundaClient camundaClient,
                             @Value("${outpass.escalate-after:PT24H}") String defaultEscalateAfter) {
        this.camundaClient = camundaClient;
        this.defaultEscalateAfter = defaultEscalateAfter;
    }

    @PostMapping("/outpass")
    public Map<String, Object> applyOutpass(@RequestBody OutpassRequest request) {
        Map<String, Object> variables = toVariables(request, defaultEscalateAfter);

        ProcessInstanceEvent instance = camundaClient.newCreateInstanceCommand()
                .bpmnProcessId(PROCESS_ID)
                .latestVersion()
                .variables(variables)
                .send()
                .join();

        return Map.of(
                "processInstanceKey", instance.getProcessInstanceKey(),
                "regNo", request.regNo());
    }

    /**
     * Rejects malformed requests with 400 before any process instance is created,
     * then maps the request to process variables.
     */
    static Map<String, Object> toVariables(OutpassRequest r, String defaultEscalateAfter) {
        requireText("studentName", r.studentName());
        requireText("regNo", r.regNo());
        requireText("hostelBlock", r.hostelBlock());
        requireText("parentPhone", r.parentPhone());
        requireDate("outDate", r.outDate());
        requireDate("returnDate", r.returnDate());

        String escalateAfter = isBlank(r.escalateAfter()) ? defaultEscalateAfter : r.escalateAfter();
        try {
            Duration.parse(escalateAfter);
        } catch (DateTimeParseException e) {
            throw badRequest("escalateAfter must be an ISO-8601 duration such as PT24H or PT2M");
        }

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("studentName", r.studentName());
        variables.put("regNo", r.regNo());
        variables.put("hostelBlock", r.hostelBlock());
        variables.put("outDate", r.outDate());
        variables.put("returnDate", r.returnDate());
        variables.put("parentPhone", r.parentPhone());
        variables.put("purpose", r.purpose() == null ? "" : r.purpose());
        variables.put("escalateAfter", escalateAfter);
        return variables;
    }

    private static void requireText(String field, String value) {
        if (isBlank(value)) {
            throw badRequest(field + " is required");
        }
    }

    private static void requireDate(String field, String value) {
        requireText(field, value);
        try {
            LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw badRequest(field + " must use the yyyy-MM-dd format");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
