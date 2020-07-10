package com.next.service;

import com.google.common.collect.Lists;
import com.next.model.TrainSeat;
import com.next.seatDao.TrainSeatMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TransactionService {

    @Resource
    private TrainSeatMapper trainSeatMapper;

    @Transactional(rollbackFor = Exception.class)
    public void batchInsertSeat(List<TrainSeat> list) {
        List<List<TrainSeat>> trainTicketPartitionList = Lists.partition(list, 1000);
        trainTicketPartitionList.parallelStream().forEach(partitionList -> {
            // 批量插入
            trainSeatMapper.batchInsert(partitionList);
        });
    }
}
