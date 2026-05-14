package com.yeswater.bids.exec.infrastructure.audit;

import com.yeswater.bids.exec.domain.model.ExecuteLog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "bids.audit.elasticsearch.enabled", havingValue = "false", matchIfMissing = true)
public class NoopAuditSearchPublisher implements AuditSearchPublisher {

    @Override
    public void publish(ExecuteLog log) {
    }
}
