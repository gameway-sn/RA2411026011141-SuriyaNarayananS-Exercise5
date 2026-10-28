package edu.srmist.outpass;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Deploys the process model and both forms to the SaaS cluster on start-up,
 * so nothing has to be deployed by hand from Web Modeler.
 */
@SpringBootApplication
@Deployment(resources = {
        "classpath:bpmn/hostel-outpass.bpmn",
        "classpath:bpmn/warden-approval.form",
        "classpath:bpmn/chief-warden-approval.form"})
public class OutpassWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(OutpassWorkflowApplication.class, args);
    }
}
