package com.yeswater.bids.exec.infrastructure.audit;

import com.yeswater.bids.exec.domain.model.ExecuteLog;

public interface AuditSearchPublisher {

    void publish(ExecuteLog log);
}
