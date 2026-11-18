package com.zhike.common;

import java.util.List;

/** 分页结果。total 是满足条件的总条数，records 是当前页。 */
public record PageResult<T>(long total, List<T> records) {

    public static <T> PageResult<T> of(long total, List<T> records) {
        return new PageResult<>(total, records);
    }
}
