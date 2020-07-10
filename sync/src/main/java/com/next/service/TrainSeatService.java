package com.next.service;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.next.dao.TrainNumberMapper;
import com.next.model.TrainNumber;
import com.next.model.TrainSeat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class TrainSeatService {

    @Resource
    private TrainCacheService trainCacheService;
    @Resource
    private TrainNumberMapper trainNumberMapper;

    public void handle(List<CanalEntry.Column> columns, CanalEntry.EventType eventType) {
        if (eventType != CanalEntry.EventType.UPDATE) {
            log.info("not update, no need care");
            return;
        }
        TrainSeat trainSeat = new TrainSeat();
        boolean isStatusUpdated = false;
        for (CanalEntry.Column column : columns) {
            if (column.getName().equals("status")) {
                trainSeat.setStatus(Integer.parseInt(column.getValue()));
                if (column.getUpdated()) {
                    isStatusUpdated = true;
                } else {
                    break;
                }
            } else if (column.getName().equals("id")) {
                trainSeat.setId(Long.parseLong(column.getValue()));
            } else if (column.getName().equals("carriage_number")) {
                trainSeat.setCarriageNumber(Integer.parseInt(column.getValue()));
            } else if (column.getName().equals("row_number")) {
                trainSeat.setRowNumber(Integer.parseInt(column.getValue()));
            } else if (column.getName().equals("seat_number")) {
                trainSeat.setSeatNumber(Integer.parseInt(column.getValue()));
            } else if (column.getName().equals("train_number_id")) {
                trainSeat.setTrainNumberId(Integer.parseInt(column.getValue()));
            } else if (column.getName().equals("id")) {
                trainSeat.setId(Long.parseLong(column.getValue()));
            } else if (column.getName().equals("ticket")) {
                trainSeat.setTicket(column.getValue());
            } else if (column.getName().equals("from_station_id")) {
                trainSeat.setFromStationId(Integer.parseInt(column.getValue()));
            } else if (column.getName().equals("to_station_id")) {
                trainSeat.setToStationId(Integer.parseInt(column.getValue()));
            }
        }
        if (!isStatusUpdated) {
            log.info("status not update, no need care");
            return;
        }
        log.info("train seat status update, trainSeat:{}", trainSeat);

        /**
         * 1、指定座位被占: hash
         * cacheKey: 车次_日期，D386_20200401
         * field: carriage_row_seat_fromStationId_toStationId
         * value: 0,空闲，1，占座
         *
         * 2、每个座位详情剩余的座位数: hash
         * cacheKey: 车次_日期_Count，D386_20200401_Count
         * field: fromStationId_toStationId
         * value: 实际座位数
         */
        TrainNumber trainNumber = trainNumberMapper.selectByPrimaryKey(trainSeat.getTrainNumberId());
        if (trainSeat.getStatus() == 1) { // 放票
            trainCacheService.hset(
                    trainNumber.getName() + "_" + trainSeat.getTicket(),
                    trainSeat.getCarriageNumber() + "_" + trainSeat.getRowNumber() + "_" + trainSeat.getSeatNumber()
                            + "_" + trainSeat.getFromStationId() + "_" + trainSeat.getToStationId(),
                    "0"
            );
            trainCacheService.hincrBy(
                    trainNumber.getName() + "_" + trainSeat.getTicket() + "_Count",
                    trainSeat.getFromStationId() + "_" + trainSeat.getToStationId(),
                    1l
            );
            log.info("seat+1, trainNumber:{}, trainSeat:{}", trainNumber.getName(), trainSeat);
        } else if (trainSeat.getStatus() == 2) { // 占票
            trainCacheService.hset(
                    trainNumber.getName() + "_" + trainSeat.getTicket(),
                    trainSeat.getCarriageNumber() + "_" + trainSeat.getRowNumber() + "_" + trainSeat.getSeatNumber()
                            + "_" + trainSeat.getFromStationId() + "_" + trainSeat.getToStationId(),
                    "1"
            );
            trainCacheService.hincrBy(
                    trainNumber.getName() + "_" + trainSeat.getTicket() + "_Count",
                    trainSeat.getFromStationId() + "_" + trainSeat.getToStationId(),
                    -1l
            );
            log.info("seat-1, trainNumber:{}, trainSeat:{}", trainNumber.getName(), trainSeat);
        } else {
            log.info("status update not 1 or 2, no need care");
        }
    }
}
