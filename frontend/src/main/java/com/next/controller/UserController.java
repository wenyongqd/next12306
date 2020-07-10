package com.next.controller;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.next.common.JsonData;
import com.next.common.RequestHolder;
import com.next.dto.TrainOrderExtDto;
import com.next.model.TrainOrder;
import com.next.model.TrainOrderDetail;
import com.next.model.TrainTraveller;
import com.next.model.TrainUser;
import com.next.service.TrainOrderService;
import com.next.service.TrainStationService;
import com.next.service.TrainTravellerService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
public class UserController {

    @Resource
    private TrainTravellerService trainTravellerService;
    @Resource
    private TrainOrderService trainOrderService;
    @Resource
    private TrainStationService trainStationService;

    @RequestMapping("/getTravellers.json")
    @ResponseBody
    public JsonData getTravellers() {
        TrainUser trainUser = RequestHolder.getCurrentUser();
        List<TrainTraveller> trainTravellerList = trainTravellerService.getByUserId(trainUser.getId());
        List<TrainTraveller> showList = trainTravellerList.stream().map(
                trainTraveller -> TrainTraveller.builder()
                        .id(trainTraveller.getId())
                        .name(trainTraveller.getName())
                        .adultFlag(trainTraveller.getAdultFlag())
                        .idNumber(hideSensitiveMsg(trainTraveller.getIdNumber()))
                        .build())
                .collect(Collectors.toList());
        return JsonData.success(showList);
    }

    private String hideSensitiveMsg(String msg) {
        if (StringUtils.isBlank(msg) || msg.length() < 7) {
            return msg;
        }
        return msg.substring(0, 3) + "******" + msg.substring(msg.length() - 3);
    }

    @RequestMapping("/getOrderList.json")
    @ResponseBody
    public JsonData getOrderList() {
        TrainUser trainUser = RequestHolder.getCurrentUser();
        List<TrainOrder> orderList = trainOrderService.getOrderList(trainUser.getId());
        if (CollectionUtils.isEmpty(orderList)) {
            return JsonData.success();
        }

        List<TrainTraveller> trainTravellerList = trainTravellerService.getByUserId(trainUser.getId());
        Map<Long, String> travellerNameMap = Maps.newHashMap();
        trainTravellerList.parallelStream().forEach(trainTraveller ->
                travellerNameMap.put(trainTraveller.getId(), trainTraveller.getName()));

        List<String> orderIdList = orderList.stream().map(order -> order.getOrderId()).collect(Collectors.toList());
        List<TrainOrderDetail> orderDetailList = trainOrderService.getOrderDetailList(orderIdList);
        // orderId -> Collection<TrainOrderDetail>
        Multimap<String, TrainOrderDetail> orderDetailMultimap = HashMultimap.create();
        orderDetailList.parallelStream().forEach(trainOrderDetail ->
                orderDetailMultimap.put(trainOrderDetail.getParentOrderId(), trainOrderDetail));

        DateTimeFormatter df = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        ZoneId zoneId = ZoneId.systemDefault();

        List<TrainOrderExtDto> dtoList = orderList.stream().map(order -> {
            TrainOrderExtDto dto = new TrainOrderExtDto();
            dto.setTrainOrder(order);
            dto.setFromStationName(trainStationService.getStationNameById(order.getFromStationId()));
            dto.setToStationName(trainStationService.getStationNameById(order.getToStationId()));
            dto.setShowPay(order.getStatus() == 10);
            dto.setShowCancel(order.getStatus() == 20);

            LocalDateTime startTime = order.getTrainStart().toInstant().atZone(zoneId).toLocalDateTime();
            LocalDateTime endTime = order.getTrainEnd().toInstant().atZone(zoneId).toLocalDateTime();
            Collection<TrainOrderDetail> tmpOrderDetailList = orderDetailMultimap.get(order.getOrderId());
            dto.setSeatInfo(startTime.format(df) + "~" + endTime.format(df) + " " +
                    generateSeatInfo(tmpOrderDetailList, travellerNameMap) + " " +
                    "(金额：" + order.getTotalMoney() + "元）");
            return dto;
        }).collect(Collectors.toList());

        return JsonData.success(dtoList);
    }

    private String generateSeatInfo(Collection<TrainOrderDetail> tmpOrderDetailList, Map<Long, String> travellerNameMap) {
        if (CollectionUtils.isEmpty(tmpOrderDetailList)) {
            return "";
        }
        int index = 0;
        StringBuilder stringBuilder = new StringBuilder(tmpOrderDetailList.size() * 20);
        for (TrainOrderDetail trainOrderDetail : tmpOrderDetailList) {
            if (trainOrderDetail.getTravellerId() == 0 || !travellerNameMap.containsKey(trainOrderDetail.getTravellerId())) {
                // 异常数据
                continue;
            }
            if (index > 0) {
                stringBuilder.append("; ");
            }
            stringBuilder.append(travellerNameMap.get(trainOrderDetail.getTravellerId())).append(" ")
                    .append(trainOrderDetail.getCarriageNumber()).append("车")
                    .append(trainOrderDetail.getRowNumber()).append("排")
                    .append(trainOrderDetail.getSeatNumber()).append("座");
            index++;
        }
        return stringBuilder.toString();
    }
}
