package com.huawei.bids.audit;

import com.huawei.bids.model.ExecuteLog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(AuditSearchPublisher.class)
public class NoopAuditSearchPublisher implements AuditSearchPublisher {

    @Override
    public void publish(ExecuteLog log) {
    }
}
