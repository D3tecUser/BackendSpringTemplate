package com.d3tec.template.nomeDoSeuProjeto.email.queue;

import com.d3tec.template.nomeDoSeuProjeto.email.model.EmailJobClaim;
import com.d3tec.template.nomeDoSeuProjeto.email.model.TransactionalEmail;

import java.time.Duration;
import java.util.Optional;

public interface EmailQueuePort {
    void enqueue(TransactionalEmail email);
    Optional<EmailJobClaim> claimNext();
    void acknowledge(EmailJobClaim claim);
    void retry(EmailJobClaim claim, Duration delay, String errorMessage);
    void moveToDeadLetter(EmailJobClaim claim, String errorMessage);
    int requeueStaleJobs();
}
