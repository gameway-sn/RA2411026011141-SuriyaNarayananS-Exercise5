package edu.srmist.outpass.api;

/**
 * JSON body of POST /outpass. Dates use yyyy-MM-dd.
 * escalateAfter is optional (ISO-8601 duration such as PT2M); when it is
 * missing the app default PT24H is used for the Chief Warden escalation timer.
 */
public record OutpassRequest(
        String studentName,
        String regNo,
        String hostelBlock,
        String outDate,
        String returnDate,
        String parentPhone,
        String purpose,
        String escalateAfter) {
}
