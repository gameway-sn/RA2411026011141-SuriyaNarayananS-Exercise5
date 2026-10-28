package edu.srmist.outpass.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Job workers for the two service tasks in hostel-outpass.bpmn.
 * Each @JobWorker type must match the task type set in the model exactly.
 * The returned map is written back as process variables and the job
 * completes automatically when the method returns.
 */
@Component
public class OutpassWorkers {

    private static final Logger log = LoggerFactory.getLogger(OutpassWorkers.class);

    /** Service task "Check request". Reads regNo, outDate, returnDate. */
    @JobWorker(type = "check-outpass")
    public Map<String, Object> checkOutpass(@Variable("regNo") String regNo,
                                            @Variable("outDate") String outDate,
                                            @Variable("returnDate") String returnDate) {
        LocalDate out = LocalDate.parse(outDate);
        LocalDate back = LocalDate.parse(returnDate);

        long nights = ChronoUnit.DAYS.between(out, back);
        boolean valid = !back.isBefore(out);
        boolean approved = valid && nights == 0; // a day out is auto-approved

        log.info("Checked out-pass for {}: {} -> {}, nights={}, valid={}, autoApproved={}",
                regNo, outDate, returnDate, nights, valid, approved);

        return Map.of("nights", nights, "valid", valid, "approved", approved);
    }

    /** Service task "Issue out-pass". Runs on every path, so it also reports rejections. */
    @JobWorker(type = "issue-outpass")
    public Map<String, Object> issueOutpass(@Variable("studentName") String studentName,
                                            @Variable("regNo") String regNo,
                                            @Variable("outDate") String outDate,
                                            @Variable("parentPhone") String parentPhone,
                                            @Variable("approved") Boolean approved,
                                            @Variable("wardenRemarks") String wardenRemarks) {
        boolean issued = Boolean.TRUE.equals(approved);
        String passStatus = issued ? "ISSUED" : "REJECTED";
        String passNumber = issued ? "OP-" + regNo + "-" + outDate : null;

        log.info("SMS to parent {}: out-pass for {} ({}) is {}{}. Remarks: {}",
                parentPhone, studentName, regNo, passStatus,
                issued ? " with pass number " + passNumber : "",
                wardenRemarks == null || wardenRemarks.isBlank() ? "-" : wardenRemarks);

        // HashMap because passNumber is null for rejected requests (Map.of rejects nulls).
        Map<String, Object> result = new HashMap<>();
        result.put("passStatus", passStatus);
        result.put("passNumber", passNumber);
        return result;
    }
}
