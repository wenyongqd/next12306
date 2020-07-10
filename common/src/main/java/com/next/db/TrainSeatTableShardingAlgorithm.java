package com.next.db;

import io.shardingsphere.api.algorithm.sharding.PreciseShardingValue;
import io.shardingsphere.api.algorithm.sharding.standard.PreciseShardingAlgorithm;

import java.util.Collection;

public class TrainSeatTableShardingAlgorithm implements PreciseShardingAlgorithm<Integer> {

    private final static String PREFIX = "train_seat_";

    private String determineTable(int val) {
        int table = val % 10;
        if (table == 0) {
            table = 10;
        }
        return PREFIX + table;
    }

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Integer> shardingValue) {
        String actualTableName = determineTable(shardingValue.getValue());
        if (availableTargetNames.contains(actualTableName)) {
            return actualTableName;
        }
        throw new IllegalArgumentException();
    }
}
