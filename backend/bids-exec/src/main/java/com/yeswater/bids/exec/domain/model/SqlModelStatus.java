package com.yeswater.bids.exec.domain.model;

public enum SqlModelStatus {
    /** 草稿态，仅允许配置和校验。 */
    DRAFT,
    /** 已发布态，允许运行态执行。 */
    PUBLISHED,
    /** 已下线态，拒绝运行态执行。 */
    OFFLINE
}
