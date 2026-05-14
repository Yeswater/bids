package com.yeswater.bids.exec.infrastructure.audit;

import com.yeswater.bids.exec.domain.model.ExecuteLog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "bids.audit.elasticsearch.enabled", havingValue = "true")
public class ElasticsearchAuditSearchPublisher implements AuditSearchPublisher {
    private final ElasticsearchOperations elasticsearchOperations;

    public ElasticsearchAuditSearchPublisher(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public void publish(ExecuteLog log) {
        ExecuteLogDocument document = new ExecuteLogDocument(
                log.executeId(),
                log.modelCode(),
                log.username(),
                log.finalSql(),
                log.parametersJson(),
                log.success(),
                log.errorMessage(),
                log.durationMs(),
                log.rowCount(),
                log.createdAt()
        );
        elasticsearchOperations.save(document);
    }
}
