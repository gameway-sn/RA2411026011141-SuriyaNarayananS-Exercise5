package edu.srmist.outpass.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Plain unit tests: no Camunda cluster needed. Run with mvn test. */
class OutpassWorkersTest {

    private final OutpassWorkers workers = new OutpassWorkers();

    @Test
    void dayOutIsValidAndAutoApproved() {
        Map<String, Object> v = workers.checkOutpass("RA2411026011139", "2026-10-20", "2026-10-20");
        assertEquals(0L, v.get("nights"));
        assertEquals(true, v.get("valid"));
        assertEquals(true, v.get("approved"));
    }

    @Test
    void overnightIsValidButNeedsWarden() {
        Map<String, Object> v = workers.checkOutpass("RA2411026011139", "2026-10-20", "2026-10-23");
        assertEquals(3L, v.get("nights"));
        assertEquals(true, v.get("valid"));
        assertEquals(false, v.get("approved"));
    }

    @Test
    void returnBeforeOutIsInvalid() {
        Map<String, Object> v = workers.checkOutpass("RA2411026011139", "2026-10-20", "2026-10-18");
        assertEquals(-2L, v.get("nights"));
        assertEquals(false, v.get("valid"));
        assertEquals(false, v.get("approved"));
    }

    @Test
    void approvedRequestGetsPassNumber() {
        Map<String, Object> v = workers.issueOutpass("Ram", "RA2411026011139", "2026-10-20",
                "9876543210", true, "Return by 8 pm");
        assertEquals("ISSUED", v.get("passStatus"));
        assertEquals("OP-RA2411026011139-2026-10-20", v.get("passNumber"));
    }

    @Test
    void rejectedRequestHasNoPassNumber() {
        Map<String, Object> v = workers.issueOutpass("Ram", "RA2411026011139", "2026-10-20",
                "9876543210", false, null);
        assertEquals("REJECTED", v.get("passStatus"));
        assertNull(v.get("passNumber"));
    }

    @Test
    void missingApprovalCountsAsRejected() {
        Map<String, Object> v = workers.issueOutpass("Ram", "RA2411026011139", "2026-10-20",
                "9876543210", null, null);
        assertEquals("REJECTED", v.get("passStatus"));
    }
}
