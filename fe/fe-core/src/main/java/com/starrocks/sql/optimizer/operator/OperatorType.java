// This file is licensed under the Elastic License 2.0. Copyright 2021-present, StarRocks Inc.

package com.starrocks.sql.optimizer.operator;

public enum OperatorType {
    /**
     * Logical operator
     */
    LOGICAL,
    LOGICAL_PROJECT,
    LOGICAL_OLAP_SCAN,
    LOGICAL_HIVE_SCAN,
    LOGICAL_FILE_SCAN,
    LOGICAL_ICEBERG_SCAN,
    LOGICAL_HUDI_SCAN,
    LOGICAL_DELTALAKE_SCAN,
    LOGICAL_SCHEMA_SCAN,
    LOGICAL_MYSQL_SCAN,
    LOGICAL_ES_SCAN,
    LOGICAL_META_SCAN,
    LOGICAL_JDBC_SCAN,
    LOGICAL_JOIN,
    LOGICAL_AGGR,
    LOGICAL_FILTER,
    LOGICAL_LIMIT,
    LOGICAL_TOPN,
    LOGICAL_APPLY,
    LOGICAL_ASSERT_ONE_ROW,
    LOGICAL_WINDOW,
    LOGICAL_UNION,
    LOGICAL_EXCEPT,
    LOGICAL_INTERSECT,
    LOGICAL_VALUES,
    LOGICAL_REPEAT,
    LOGICAL_TABLE_FUNCTION,
    LOGICAL_CTE_ANCHOR,
    LOGICAL_CTE_PRODUCE,
    LOGICAL_CTE_CONSUME,

    /**
     * Physical operator
     */
    PHYSICAL,
    PHYSICAL_DISTRIBUTION,
    PHYSICAL_HASH_AGG,
    PHYSICAL_HASH_JOIN,
    PHYSICAL_MERGE_JOIN,
    PHYSICAL_NESTLOOP_JOIN,
    PHYSICAL_OLAP_SCAN,
    PHYSICAL_HIVE_SCAN,
    PHYSICAL_FILE_SCAN,
    PHYSICAL_ICEBERG_SCAN,
    PHYSICAL_HUDI_SCAN,
    PHYSICAL_DELTALAKE_SCAN,
    PHYSICAL_SCHEMA_SCAN,
    PHYSICAL_MYSQL_SCAN,
    PHYSICAL_META_SCAN,
    PHYSICAL_ES_SCAN,
    PHYSICAL_JDBC_SCAN,
    PHYSICAL_PROJECT,
    PHYSICAL_SORT,
    PHYSICAL_TOPN,
    PHYSICAL_UNION,
    PHYSICAL_EXCEPT,
    PHYSICAL_INTERSECT,
    PHYSICAL_ASSERT_ONE_ROW,
    PHYSICAL_WINDOW,
    PHYSICAL_VALUES,
    PHYSICAL_REPEAT,
    PHYSICAL_FILTER,
    PHYSICAL_TABLE_FUNCTION,
    PHYSICAL_DECODE,
    PHYSICAL_LIMIT,
    PHYSICAL_CTE_ANCHOR,
    PHYSICAL_CTE_PRODUCE,
    PHYSICAL_CTE_CONSUME,
    PHYSICAL_NO_CTE,

    PHYSICAL_STREAM_SCAN,
    PHYSICAL_STREAM_JOIN,
    PHYSICAL_STREAM_AGG,

    /**
     * Scalar operator
     */
    SCALAR,
    ARRAY,
    COLLECTION_ELEMENT,
    ARRAY_SLICE,
    VARIABLE,
    CONSTANT,
    CALL,
    BETWEEN,
    BINARY,
    COMPOUND,
    EXISTS,
    IN,
    IS_NULL,
    LIKE,
    DICT_MAPPING,
    CLONE,
    LAMBDA_FUNCTION,
    SUBQUERY,
    SUBFIELD,

    /**
     * PATTERN
     */
    PATTERN,
    PATTERN_LEAF,
    PATTERN_MULTI_LEAF,
    // for all type scan node
    PATTERN_SCAN,
    // for extracting pattern like this
    //     join
    //    /    \
    //  join   table
    //  /  \
    // table table
    PATTERN_MULTIJOIN,
}
