package com.next.controller;

import com.next.beans.PageQuery;
import com.next.beans.PageResult;
import com.next.common.JsonData;
import com.next.dto.TrainSeatDto;
import com.next.model.TrainSeat;
import com.next.model.TrainStation;
import com.next.param.GenerateTicketParam;
import com.next.param.PublishTicketParam;
import com.next.param.TrainSeatSearchParam;
import com.next.service.TrainSeatService;
import com.next.service.TrainStationService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/train/seat")
public class TrainSeatController {

    @Resource
    private TrainSeatService trainSeatService;
    @Resource
    private TrainStationService trainStationService;

    @RequestMapping("/list.page")
    public ModelAndView page() {
        return new ModelAndView("trainSeat");
    }

    @RequestMapping("/search.json")
    @ResponseBody
    public JsonData search(TrainSeatSearchParam param, PageQuery pageQuery) {
        int total = trainSeatService.countList(param);
        if (total == 0) {
            return JsonData.success(PageResult.<TrainSeatDto>builder().total(0).build());
        }
        List<TrainSeat> seatList = trainSeatService.searchList(param, pageQuery);
        if (CollectionUtils.isEmpty(seatList)) {
            return JsonData.success(PageResult.<TrainSeatDto>builder().total(total).build());
        }

        List<TrainStation> stationList = trainStationService.getAll();
        Map<Integer, String> stationMap = stationList.stream().collect(Collectors.toMap(TrainStation::getId, TrainStation::getName));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        ZoneId zoneId = ZoneId.systemDefault();

        List<TrainSeatDto> dtoList = seatList.stream().map(trainSeat -> {
            TrainSeatDto dto = new TrainSeatDto();
            dto.setId(trainSeat.getId());
            dto.setFromStationId(trainSeat.getFromStationId());
            dto.setFromStation(stationMap.get(trainSeat.getFromStationId()));
            dto.setToStationId(trainSeat.getToStationId());
            dto.setToStation(stationMap.get(trainSeat.getToStationId()));
            dto.setTrainNumberId(trainSeat.getTrainNumberId());
            dto.setTrainNumber(param.getTrainNumber());
            dto.setShowStart(LocalDateTime.ofInstant(trainSeat.getTrainStart().toInstant(), zoneId).format(formatter));
            dto.setShowEnd(LocalDateTime.ofInstant(trainSeat.getTrainEnd().toInstant(), zoneId).format(formatter));
            dto.setStatus(trainSeat.getStatus());
            dto.setSeatLevel(trainSeat.getSeatLevel());
            dto.setCarriageNumber(trainSeat.getCarriageNumber());
            dto.setRowNumber(trainSeat.getRowNumber());
            dto.setSeatNumber(trainSeat.getSeatNumber());
            dto.setMoney(trainSeat.getMoney());
            return dto;
        }).collect(Collectors.toList());

        return JsonData.success(PageResult.<TrainSeatDto>builder().data(dtoList).total(total).build());
    }

    @RequestMapping("/generate.json")
    @ResponseBody
    public JsonData generate(GenerateTicketParam param) {
        trainSeatService.generate(param);
        return JsonData.success();
    }

    @RequestMapping("/publish.json")
    @ResponseBody
    public JsonData publish(PublishTicketParam param) {
        trainSeatService.publish(param);
        return JsonData.success();
    }
}
