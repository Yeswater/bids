package com.huawei.bids.audit;

import com.huawei.bids.model.ExecuteLog;

public interface AuditSearchPublisher {

    void publish(ExecuteLog log);
}
