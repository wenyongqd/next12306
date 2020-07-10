package com.next.service;

import com.next.model.TrainOrder;
import com.next.model.TrainOrderDetail;
import com.next.orderDao.TrainOrderDetailMapper;
import com.next.orderDao.TrainOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TransactionService {

    @Resource
    private TrainOrderMapper trainOrderMapper;
    @Resource
    private TrainOrderDetailMapper trainOrderDetailMapper;

    @Transactional(rollbackFor = Exception.class)
    public void saveOrder(TrainOrder trainOrder, List<TrainOrderDetail> orderDetailList) {
        for(TrainOrderDetail orderDetail : orderDetailList) {
            trainOrderDetailMapper.insertSelective(orderDetail);
        }
        trainOrderMapper.insertSelective(trainOrder);
    }
}
