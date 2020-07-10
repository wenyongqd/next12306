package com.next.common;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import javafx.util.Pair;

import java.util.List;
import java.util.Map;

public class TrainTypeSeatConstant {

    // <车厢carriage、排row、座位seat pair >
    private static Table<Integer, Integer, Pair<Integer, Integer>> crh2Table = HashBasedTable.create();
    // 特殊的<车厢carriage、排row、座位seat>, 默认为5，不等于5的时才存储进来
    private static Table<Integer, Integer, Integer> crh2SpecialTable = HashBasedTable.create();
    // 车厢数与下角标对应，每一节车厢的排数
    private static List<Integer> crh2CarriageRowTotal = Lists.newArrayList(0, 11, 20, 17, 20, 11, 20, 13, 13, 11, 20, 17, 20, 11, 20, 13, 13);

    // <车厢carriage、排row、座位seat pair >
    private static Table<Integer, Integer, Pair<Integer, Integer>> crh5Table = HashBasedTable.create();
    // 特殊的<车厢carriage、排row、座位seat>, 默认为5，不等于5的时才存储进来
    private static Table<Integer, Integer, Integer> crh5SpecialTable = HashBasedTable.create();
    // 车厢数与下角标对应，每一节车厢的排数
    private static List<Integer> crh5CarriageRowTotal = Lists.newArrayList(0, 15, 19, 19, 19, 19, 9, 16, 15, 15, 19, 19, 19, 19, 9, 16, 15);

    private static Map<TrainType, Table<Integer, Integer, Pair<Integer, Integer>>> carriageMap = Maps.newConcurrentMap();

    // 车厢座位等级： <类型、车厢carriage、座位等级>
    private static Table<TrainType, Integer, TrainSeatLevel> seatCarriageLevelTable = HashBasedTable.create();

    static {
        // crh2 初始化
        for (int row = 1; row <= 12; row++) {
            crh2SpecialTable.put(7, row, 4);
            crh2SpecialTable.put(15, row, 4);
        }
        crh2SpecialTable.put(7, 13, 3);
        crh2SpecialTable.put(7, 13, 3);
        crh2SpecialTable.put(8, 1, 4);
        crh2SpecialTable.put(16, 1, 4);

        for(int carriage = 1; carriage < crh2CarriageRowTotal.size(); carriage++) { // 遍历每一节车厢
            int order = 0;
            for (int row = 1; row <= crh2CarriageRowTotal.get(carriage); row++) { // 遍历每个车厢的每一排
                // 处理每一排的座位
                int count = 5;
                if (crh2SpecialTable.contains(carriage, row)) {
                    count = crh2SpecialTable.get(carriage, row);
                }
                crh2Table.put(carriage, row, new Pair<>(order + 1, order + count));
                order += count;
            }
        }

        // crh5 初始化
        for (int row = 1; row <= 15; row++) {
            crh5SpecialTable.put(8, row, 4);
            crh5SpecialTable.put(16, row, 4);
        }
        crh5SpecialTable.put(1, 1, 4);
        crh5SpecialTable.put(2, 1, 3);
        crh5SpecialTable.put(3, 1, 3);
        crh5SpecialTable.put(4, 1, 3);
        crh5SpecialTable.put(5, 1, 3);
        crh5SpecialTable.put(6, 1, 3);
        crh5SpecialTable.put(6, 9, 4);
        crh5SpecialTable.put(7, 1, 3);
        crh5SpecialTable.put(7, 16, 1);

        crh5SpecialTable.put(9, 1, 4);
        crh5SpecialTable.put(10, 1, 3);
        crh5SpecialTable.put(11, 1, 3);
        crh5SpecialTable.put(12, 1, 3);
        crh5SpecialTable.put(13, 1, 3);
        crh5SpecialTable.put(14, 1, 3);
        crh5SpecialTable.put(14, 9, 4);
        crh5SpecialTable.put(15, 1, 3);
        crh5SpecialTable.put(15, 16, 1);

        for(int carriage = 1; carriage < crh5CarriageRowTotal.size(); carriage++) { // 遍历每一节车厢
            int order = 0;
            for (int row = 1; row <= crh5CarriageRowTotal.get(carriage); row++) { // 遍历每个车厢的每一排
                // 处理每一排的座位
                int count = 5;
                if (crh5SpecialTable.contains(carriage, row)) {
                    count = crh5SpecialTable.get(carriage, row);
                }
                crh5Table.put(carriage, row, new Pair<>(order + 1, order + count));
                order += count;
            }
        }

        carriageMap.put(TrainType.CRH2, crh2Table);
        carriageMap.put(TrainType.CRH5, crh5Table);

        seatCarriageLevelTable.put(TrainType.CRH2, 1, TrainSeatLevel.TOP_GRADE);
        seatCarriageLevelTable.put(TrainType.CRH2, 2, TrainSeatLevel.GRADE_1);

        seatCarriageLevelTable.put(TrainType.CRH5, crh5CarriageRowTotal.size() - 1, TrainSeatLevel.TOP_GRADE);
        seatCarriageLevelTable.put(TrainType.CRH5, crh5CarriageRowTotal.size() - 2, TrainSeatLevel.GRADE_1);
    }

    public static Table<Integer, Integer, Pair<Integer, Integer>> getTable(TrainType type) {
        return carriageMap.get(type);
    }

    public static TrainSeatLevel getSeatLevel(TrainType trainType, Integer carriage) {
        if (seatCarriageLevelTable.contains(trainType, carriage)) {
            return seatCarriageLevelTable.get(trainType, carriage);
        }
        return TrainSeatLevel.GRADE_2;
    }
}
