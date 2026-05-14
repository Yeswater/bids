package com.yeswater.bids.config.domain.model;

import java.util.List;

public record SqlModelConfig(
        SqlModel model,
        List<FormField> fields,
        List<ResultColumn> columns,
        List<ModelPermission> permissions
) {
}
