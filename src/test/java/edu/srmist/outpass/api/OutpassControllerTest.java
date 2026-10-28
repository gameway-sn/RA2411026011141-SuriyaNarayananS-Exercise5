package edu.srmist.outpass.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Checks request validation and variable mapping without a Camunda cluster. */
class OutpassControllerTest {

    private static OutpassRequest request(String outDate, String returnDate, String escalateAfter) {
        return new OutpassRequest("Ram", "RA2411026011139", "Block A", outDate, returnDate,
                "9876543210", "Home visit", escalateAfter);
    }

    @Test
    void usesDefaultEscalationWhenNotGiven() {
        Map<String, Object> v = OutpassController.toVariables(request("2026-10-20", "2026-10-22", null), "PT24H");
        assertEquals("PT24H", v.get("escalateAfter"));
        assertEquals("Block A", v.get("hostelBlock"));
        assertEquals(8, v.size());
    }

    @Test
    void keepsEscalationOverride() {
        Map<String, Object> v = OutpassController.toVariables(request("2026-10-20", "2026-10-22", "PT2M"), "PT24H");
        assertEquals("PT2M", v.get("escalateAfter"));
    }

    @Test
    void rejectsBadDateFormat() {
        assertThrows(ResponseStatusException.class,
                () -> OutpassController.toVariables(request("20-10-2026", "2026-10-22", null), "PT24H"));
    }

    @Test
    void rejectsBadDuration() {
        assertThrows(ResponseStatusException.class,
                () -> OutpassController.toVariables(request("2026-10-20", "2026-10-22", "2 minutes"), "PT24H"));
    }

    @Test
    void rejectsMissingRegNo() {
        OutpassRequest r = new OutpassRequest("Ram", " ", "Block A", "2026-10-20", "2026-10-20",
                "9876543210", null, null);
        assertThrows(ResponseStatusException.class, () -> OutpassController.toVariables(r, "PT24H"));
    }
}
