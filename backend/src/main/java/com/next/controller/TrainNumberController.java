package com.next.controller;

import com.next.common.JsonData;
import com.next.dto.TrainNumberDto;
import com.next.model.TrainNumber;
import com.next.model.TrainStation;
import com.next.param.TrainNumberParam;
import com.next.service.TrainNumberService;
import com.next.service.TrainStationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/train/number")
public class TrainNumberController {

    @Resource
    private TrainNumberService trainNumberService;
    @Resource
    private TrainStationService trainStationService;

    @RequestMapping("/list.page")
    public ModelAndView page() {
        return new ModelAndView("trainNumber");
    }

    @RequestMapping("/list.json")
    @ResponseBody
    public JsonData list() {
        List<TrainNumber> numberList = trainNumberService.getAll();
        List<TrainStation> stationList = trainStationService.getAll();
        Map<Integer, String> stationMap = stationList.stream().collect(Collectors.toMap(TrainStation::getId, TrainStation::getName));
        List<TrainNumberDto> dtoList = numberList.stream().map(number -> {
            TrainNumberDto dto = new TrainNumberDto();
            dto.setId(number.getId());
            dto.setFromStationId(number.getFromStationId());
            dto.setToStationId(number.getToStationId());
            dto.setFromStation(stationMap.get(number.getFromStationId()));
            dto.setToStation(stationMap.get(number.getToStationId()));
            dto.setFromCityId(number.getFromCityId());
            dto.setToCityId(number.getToCityId());
            dto.setName(number.getName());
            dto.setTrainType(number.getTrainType());
            dto.setType(number.getType());
            dto.setSeatNum(number.getSeatNum());
            return dto;
        }).collect(Collectors.toList());
        return JsonData.success(dtoList);
    }

    @RequestMapping("/save.json")
    @ResponseBody
    public JsonData save(TrainNumberParam param) {
        trainNumberService.save(param);
        return JsonData.success();
    }

    @RequestMapping("/update.json")
    @ResponseBody
    public JsonData update(TrainNumberParam param) {
        trainNumberService.update(param);
        return JsonData.success();
    }
}
