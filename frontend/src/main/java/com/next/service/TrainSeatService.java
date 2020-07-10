package com.next.service;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.next.common.TrainEsConstant;
import com.next.common.TrainSeatLevel;
import com.next.common.TrainType;
import com.next.common.TrainTypeSeatConstant;
import com.next.dto.RollbackSeatDto;
import com.next.dto.TrainNumberLeftDto;
import com.next.dto.TrainOrderDto;
import com.next.exception.BusinessException;
import com.next.exception.ParamException;
import com.next.model.TrainNumber;
import com.next.model.TrainNumberDetail;
import com.next.model.TrainOrder;
import com.next.model.TrainOrderDetail;
import com.next.model.TrainSeat;
import com.next.model.TrainUser;
import com.next.mq.MessageBody;
import com.next.mq.QueueTopic;
import com.next.mq.RabbitMqClient;
import com.next.orderDao.TrainOrderDetailMapper;
import com.next.orderDao.TrainOrderMapper;
import com.next.param.GrabTicketParam;
import com.next.param.SearchLeftCountParam;
import com.next.seatDao.TrainSeatMapper;
import com.next.util.BeanValidator;
import com.next.util.JsonMapper;
import com.next.util.StringUtil;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.codehaus.jackson.type.TypeReference;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TrainSeatService {

    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(
      5, 20, 2, TimeUnit.MINUTES,
            new ArrayBlockingQueue<Runnable>(200), new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @Resource
    private EsClient esClient;
    @Resource
    private TrainNumberService trainNumberService;
    @Resource
    private TrainCacheService trainCacheService;
    @Resource
    private TrainSeatMapper trainSeatMapper;
    @Resource
    private RabbitMqClient rabbitMqClient;
    @Resource
    private TrainOrderMapper trainOrderMapper;
    @Resource
    private TrainOrderDetailMapper trainOrderDetailMapper;
    @Resource
    private TransactionService transactionService;

    public List<TrainNumberLeftDto> searchLeftCount(SearchLeftCountParam param) throws Exception {
        BeanValidator.check(param);
        List<TrainNumberLeftDto> dtoList = Lists.newArrayList();

        // 从es里获取满足条件的车次
        GetRequest getRequest = new GetRequest(TrainEsConstant.INDEX, TrainEsConstant.TYPE,
                param.getFromStationId() + "_" + param.getToStationId());
        GetResponse getResponse = esClient.get(getRequest);
        if (getResponse == null) {
            throw new BusinessException("数据查询失败，请重试");
        }
        Map<String, Object> map = getResponse.getSourceAsMap();
        if (MapUtils.isEmpty(map)) {
            return dtoList;
        }

        String trainNumbers = (String)map.get(TrainEsConstant.COLUMN_TRAIN_NUMBER);// D9,D386

        // 拆分出所有的车次
        List<String> numberList = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(trainNumbers);

        numberList.parallelStream().forEach(number -> {
            TrainNumber trainNumber = trainNumberService.findByNameFromCache(number);
            if (trainNumber == null) {
                return;
            }

            String detailStr = trainCacheService.get("TN_" + number);
            List<TrainNumberDetail> detailList = JsonMapper.string2Obj(detailStr, new TypeReference<List<TrainNumberDetail>>() {
            });

            Map<Integer, TrainNumberDetail> detailMap = Maps.newHashMap();
            detailList.stream().forEach(detail -> detailMap.put(detail.getFromStationId(), detail));

            /**
             * detailList: {1,2},{2,3},{3,4},{4,5},{5,6}
             * detailMap: 1->{1,2}, 2->{2,3}, ... 5->{5,6}
             * param: 2->5
             * target: {2,3},{3,4},{4,5}
             * detailMap 2->{2,3} -> 3-> {3,4}->4->{4,5}
             *
             * {2,3}:5,{3,4}:3,{4,5}:10 ->  left:3
             */
            int curFromStationId = param.getFromStationId();
            int targetToStationId = param.getToStationId();
            long min = Long.MAX_VALUE;
            boolean isSuccess = false;
            String redisKey = number + "_" + param.getDate() + "_Count";

            while(true) {
                TrainNumberDetail detail = detailMap.get(curFromStationId);
                if (detail == null) {
                    log.error("detail is null, stationId:{}, number:{}", curFromStationId, number);
                    break;
                }

                // 从redis里取出本段详情剩余的座位，并更新整体的最小座位数
                min = Math.min(min, NumberUtils.toLong(trainCacheService.hget(redisKey, detail.getFromStationId() + "_" + detail.getToStationId()), 0l));

                if (detail.getToStationId() == targetToStationId) {
                    isSuccess = true;
                    break;
                }

                // 下次查询的起始站是本次详情的到达站
                curFromStationId = detail.getToStationId();
            }
            if (isSuccess) {
                dtoList.add(new TrainNumberLeftDto(trainNumber.getId(), number, min));
            }
        });
        return dtoList;
    }

    public TrainOrderDto grabTicket(GrabTicketParam param, TrainUser trainUser) {
        BeanValidator.check(param);

        // 乘车人
        List<Long> travellerIdList = StringUtil.splitToListLong(param.getTravellerIds());
        if (CollectionUtils.isEmpty(travellerIdList)) {
            throw new ParamException("必须指定乘车人");
        }

        // 车次
        TrainNumber trainNumber = trainNumberService.findByNameFromCache(param.getNumber());
        if (trainNumber == null) {
            throw new ParamException("车次不存在");
        }

        String detailStr = trainCacheService.get("TN_" + param.getNumber());
        List<TrainNumberDetail> detailList = JsonMapper.string2Obj(detailStr, new TypeReference<List<TrainNumberDetail>>() {
        });

        Map<Integer, TrainNumberDetail> detailMap = Maps.newHashMap();
        detailList.stream().forEach(detail -> detailMap.put(detail.getFromStationId(), detail));

        // 实际涉及的车次详情
        List<TrainNumberDetail> targetDetailList = Lists.newArrayList();
        int curFromStationId = param.getFromStationId();
        int targetToStationId = param.getToStationId();

        while(true) {
            TrainNumberDetail detail = detailMap.get(curFromStationId);
            if (detail == null) {
                log.error("detail is null, stationId:{}, number:{}", curFromStationId, param.getNumber());
                throw new ParamException("车次详情数据有误，请联系管理员");
            }
            targetDetailList.add(detail);
            if (detail.getToStationId() == targetToStationId) {
                break;
            }
            // 下次查询的起始站是本次详情的到达站
            curFromStationId = detail.getToStationId();
        }

        // 实际车次座位情况
        String redisKey = param.getNumber() + "_" + param.getDate();
        Map<String, String> seatMap = trainCacheService.hgetAll(redisKey);

        // 指定车次座位布局情况
        TrainType trainType = TrainType.valueOf(trainNumber.getTrainType());
        Table<Integer, Integer, Pair<Integer, Integer>> seatTable = TrainTypeSeatConstant.getTable(trainType);

        String parentOrderId = UUID.randomUUID().toString(); // 主订单号
        List<TrainOrderDetail> orderDetailList = Lists.newArrayList(); // 生成的订单详情
        List<TrainSeat> seatList = Lists.newArrayList(); // 最终抢到的座位

        Integer totalMoney = 0;
        for(Long travellerId : travellerIdList) { // 给每一个乘客生成一个座位，并组成一个订单
            // 先筛选出一个符合要求的座位（已经占座了）
            TrainSeat tmpTrainSeat = selectOneMatch(seatTable, seatMap, targetDetailList, trainNumber, travellerId, trainUser.getId(), param.getDate());
            if (tmpTrainSeat == null) {
                break;
            }

            TrainSeatLevel seatLevel = TrainTypeSeatConstant.getSeatLevel(trainType, tmpTrainSeat.getCarriageNumber());
            // 生成订单详情
            TrainOrderDetail trainOrderDetail = TrainOrderDetail.builder()
                    .parentOrderId(parentOrderId)
                    .orderId(UUID.randomUUID().toString())
                    .userId(trainUser.getId())
                    .travellerId(travellerId)
                    .ticket(param.getDate())
                    .trainNumberId(trainNumber.getId())
                    .fromStationId(param.getFromStationId())
                    .toStationId(param.getToStationId())
                    .carriageNumber(tmpTrainSeat.getCarriageNumber())
                    .rowNumber(tmpTrainSeat.getRowNumber())
                    .seatNumber(tmpTrainSeat.getSeatNumber())
                    .seatLevel(seatLevel.getLevel())
                    .trainStart(tmpTrainSeat.getTrainStart())
                    .trainEnd(tmpTrainSeat.getTrainEnd())
                    .money(tmpTrainSeat.getMoney())
                    .showNumber("")
                    .status(10)
                    .createTime(new Date())
                    .updateTime(new Date())
                    .expireTime(DateUtils.addMinutes(new Date(), 30))
                    .build();

            totalMoney += tmpTrainSeat.getMoney();
            orderDetailList.add(trainOrderDetail);
            seatList.add(tmpTrainSeat);
        }
        if (seatList.size() < travellerIdList.size()) { // 已有座位满足不了需求，回滚占座
            rollbackPlace(seatList, targetDetailList);
            throw new BusinessException("座位不足");
        }

        // 生成主订单
        TrainOrder trainOrder = TrainOrder.builder()
                .orderId(parentOrderId)
                .userId(trainUser.getId())
                .ticket(param.getDate())
                .trainNumberId(trainNumber.getId())
                .fromStationId(param.getFromStationId())
                .toStationId(param.getToStationId())
                .trainStart(orderDetailList.get(0).getTrainStart())
                .trainEnd(orderDetailList.get(0).getTrainEnd())
                .totalMoney(totalMoney)
                .status(10)
                .createTime(new Date())
                .updateTime(new Date())
                .expireTime(DateUtils.addMinutes(new Date(), 30))
                .build();

        // 保存订单及详情（注意：事务型保存）
        try {
            transactionService.saveOrder(trainOrder, orderDetailList);
        } catch (Exception e) {
            rollbackPlace(seatList, targetDetailList);
            log.error("saveOrder exception, trainOrder:{}, orderDetailList:{}", trainOrder, JsonMapper.obj2String(orderDetailList), e);
            throw new BusinessException("生成订单失败");
        }

        log.info("saveOrder success, order:{}", trainOrder);

        // 发送订单创建成功消息：处理消息时候给用户发短信等等
        MessageBody messageBody1 = new MessageBody();
        messageBody1.setTopic(QueueTopic.ORDER_CREATE);
        messageBody1.setDetail(JsonMapper.obj2String(trainOrder));
        rabbitMqClient.send(messageBody1);

        // 发送订单支付延迟检查消息
        MessageBody messageBody2 = new MessageBody();
        messageBody2.setTopic(QueueTopic.ORDER_PAY_DELAY_CHECK);
        messageBody2.setDetail(JsonMapper.obj2String(trainOrder));
        messageBody2.setDelay(30 * 60 * 1000);
        rabbitMqClient.sendDelay(messageBody2, 30 * 60 * 1000);

        // 返回核心订单数据
        return TrainOrderDto.builder()
                .trainOrder(trainOrder)
                .trainOrderDetailList(orderDetailList)
                .build();
    }

    // 注意：这种写法是错误的，一定要重视
    @Transactional(rollbackFor = Exception.class)
    public void saveOrder(TrainOrder trainOrder, List<TrainOrderDetail> orderDetailList) {
        for(TrainOrderDetail orderDetail : orderDetailList) {
            trainOrderDetailMapper.insertSelective(orderDetail);
        }
        trainOrderMapper.insertSelective(trainOrder);
    }

    // 筛选出一个符合要求的座位：carriage\row\seat 在指定的车次详情里都是空着，并且完成占座
    private TrainSeat selectOneMatch(Table<Integer, Integer, Pair<Integer, Integer>> seatTable,
                                     Map<String, String> seatMap,
                                     List<TrainNumberDetail> targetDetailList,
                                     TrainNumber trainNumber,
                                     Long travellerId, Long userId,
                                     String ticket) {
        for(Table.Cell<Integer, Integer, Pair<Integer, Integer>> cell : seatTable.cellSet()) { // 遍历 每一节车厢 每一排
            Integer carriage = cell.getRowKey(); //当前车厢，获取座位数及座位等级的
            Integer row = cell.getColumnKey(); // 当前车厢的排，锁定座位数
            Pair<Integer, Integer> rowSeatRange = seatTable.get(carriage, row); // 获取指定车厢指定排的座位号的范围

            for (int index = rowSeatRange.getKey(); index <= rowSeatRange.getValue(); index++) { // 遍历每一排的每个座位号,看是否可提供票
                int cnt = 0;
                for (TrainNumberDetail detail : targetDetailList) {
                    String cacheKey = carriage + "_" + row + "_" + index + "_" + detail.getFromStationId() + "_" + detail.getToStationId();
                    if (!seatMap.containsKey(cacheKey) || NumberUtils.toInt(seatMap.get(cacheKey), 0) != 0) { // 无座或者已经被占了
                        break;
                    }
                    cnt ++;
                }
                if (cnt == targetDetailList.size()) { // 这个座位在本次需要的所有站点都是空闲的，可以尝试占座
                    // 数据检查通过，尝试更新DB进行占座
                    TrainSeat trainSeat = TrainSeat.builder()
                            .carriageNumber(carriage)
                            .rowNumber(row)
                            .seatNumber(index)
                            .trainNumberId(trainNumber.getId())
                            .travellerId(travellerId)
                            .userId(userId)
                            .build();
                    try {
                        trainSeat = place(trainSeat, targetDetailList, ticket);
                        if (trainSeat != null) {
                            log.info("place success, {}", trainSeat);
                            for (TrainNumberDetail detail : targetDetailList) {
                                seatMap.put(carriage + "_" + row + "_" + index + "_" + detail.getFromStationId() + "_" + detail.getToStationId(), "1");
                            }
                            return trainSeat;
                        }
                    } catch (BusinessException e) {
                        log.error("place BusinessException, {},{}", trainSeat, e.getMessage());
                        for (TrainNumberDetail detail : targetDetailList) {
                            seatMap.put(carriage + "_" + row + "_" + index + "_" + detail.getFromStationId() + "_" + detail.getToStationId(), "1");
                        }
                    } catch (Exception e2) {
                        log.error("place Exception, {}", trainSeat, e2);
                        return null;
                    }
                }
            }
        }
        return null;
    }

    // 占座：carriage\row\seat
    // 比如：1车厢1排座位1，占 北京-唐山、唐山-锦州 两段区间
    // 返回：1车厢1排座位1 在 北京-锦州 的综合座位信息，比如：开始-到达时间、座位的金额等
    private TrainSeat place(TrainSeat trainSeat, List<TrainNumberDetail> targetDetailList, String ticket) {
        List<Integer> fromStationIdList = targetDetailList.stream().map(detail -> detail.getFromStationId()).collect(Collectors.toList());
        List<TrainSeat> toUpdateSeatList = trainSeatMapper.getToPlaceSeatList(
                trainSeat.getTrainNumberId(), trainSeat.getCarriageNumber(), trainSeat.getRowNumber(),
                trainSeat.getSeatNumber(), fromStationIdList);
        if (targetDetailList.size() != toUpdateSeatList.size()) { // 要占座的座位不够，可能是某个车次详情的座位刚才被占了
            return null;
        }

        List<Long> idList = toUpdateSeatList.stream().map(TrainSeat::getId).collect(Collectors.toList());
        int ret = trainSeatMapper.batchPlace(trainSeat.getTrainNumberId(), idList, trainSeat.getTravellerId(), trainSeat.getUserId());
        if (ret != idList.size()) {
            rollbackPlace(toUpdateSeatList, targetDetailList);
            throw new BusinessException("座位被占，" + trainSeat.toString());
        }

        return TrainSeat.builder()
                .carriageNumber(trainSeat.getCarriageNumber())
                .rowNumber(trainSeat.getRowNumber())
                .seatNumber(trainSeat.getSeatNumber())
                .trainNumberId(trainSeat.getTrainNumberId())
                .travellerId(trainSeat.getTravellerId())
                .userId(trainSeat.getUserId())
                .trainStart(toUpdateSeatList.get(0).getTrainStart())
                .trainEnd(toUpdateSeatList.get(toUpdateSeatList.size() - 1).getTrainEnd())
                .money(toUpdateSeatList.stream().collect(Collectors.summingInt(TrainSeat::getMoney)))
                .build();
    }

    // 回滚占座
    private void rollbackPlace(List<TrainSeat> seatList, List<TrainNumberDetail> targetDetailList) {
        // 异步，优先保证用户线程快速完成，使用线程池（每个都不能丢弃）
        executor.submit(() -> {
            for (TrainSeat trainSeat : seatList) {
                log.info("rollback seat, seat:{}", trainSeat);
                List<Integer> fromStationIdList = targetDetailList.stream().map(TrainNumberDetail::getFromStationId).collect(Collectors.toList());
                batchRollbackSeat(trainSeat, fromStationIdList, 0);
            }
        });
    }

    public void batchRollbackSeat(TrainSeat trainSeat, List<Integer> fromStationIdList, int delayMillSeconds) {
        try {
            trainSeatMapper.batchRollbackPlace(trainSeat, fromStationIdList);
        } catch (Exception e) {
            log.error("batchRollbackSeat exception, seat:{}", trainSeat, e);

            RollbackSeatDto dto = new RollbackSeatDto();
            dto.setTrainSeat(trainSeat);
            dto.setFromStationIdList(fromStationIdList);

            MessageBody messageBody = new MessageBody();
            messageBody.setTopic(QueueTopic.SEAT_PLACE_ROLLBACK);
            messageBody.setDetail(JsonMapper.obj2String(dto));

            delayMillSeconds = delayMillSeconds * 2 + 1000;
            messageBody.setDelay(delayMillSeconds);

            rabbitMqClient.sendDelay(messageBody, delayMillSeconds);
        }
    }
}
