package com.next.mq;

import com.google.common.collect.Lists;
import com.next.model.TrainOrder;
import com.next.model.TrainOrderDetail;
import com.next.orderDao.TrainOrderDetailMapper;
import com.next.seatDao.TrainSeatMapper;
import com.next.util.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
@Slf4j
public class RabbitMqServer {

    @Resource
    private TrainOrderDetailMapper trainOrderDetailMapper;
    @Resource
    private TrainSeatMapper trainSeatMapper;

    @RabbitListener(queues = QueueConstants.COMMON_QUEUE)
    public void receive(String message) {
        log.info("common queue receive message, {}", message);

        try {
            MessageBody messageBody = JsonMapper.string2Obj(message, new TypeReference<MessageBody>() {
            });

            if (messageBody == null) {
                return;
            }

            switch (messageBody.getTopic()) {
                case QueueTopic.ORDER_CREATE:
                    log.info("order create message:{}", message);
                    // 比如给用户发短信，让他尽早支付

                    break;
                case QueueTopic.ORDER_CANCEL:
                    log.info("order cancel message:{}", message);
                    TrainOrder trainOrder = JsonMapper.string2Obj(messageBody.getDetail(), new TypeReference<TrainOrder>() {
                    });
                    List<TrainOrderDetail> orderDetailList = trainOrderDetailMapper.getByParentOrderIdList(Lists.newArrayList(trainOrder.getOrderId()));
                    for(TrainOrderDetail trainOrderDetail : orderDetailList) {
                        trainSeatMapper.cancelSeat(trainOrderDetail.getTrainNumberId(), trainOrderDetail.getTicket(),
                                trainOrderDetail.getCarriageNumber(), trainOrderDetail.getRowNumber(), trainOrderDetail.getSeatNumber(),
                                trainOrderDetail.getUserId(), trainOrderDetail.getTravellerId());
                    }
                    log.info("order cancel seat success, message:{}", message);
                    break;
                case QueueTopic.ORDER_PAY_SUCCESS:
                    log.info("order pay success, message:{}", message);
                    break;
                default:
                    log.warn("common queue receive message, {}, no need handle", message);
            }
        } catch (Exception e) {
            log.error("common queue message handle exception, msg:{}", message, e);
        }
    }
}
