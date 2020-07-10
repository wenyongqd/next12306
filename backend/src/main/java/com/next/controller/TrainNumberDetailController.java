package com.next.controller;

import com.next.common.JsonData;
import com.next.dto.TrainNumberDetailDto;
import com.next.model.TrainNumber;
import com.next.model.TrainNumberDetail;
import com.next.model.TrainStation;
import com.next.param.TrainNumberDetailParam;
import com.next.service.TrainNumberDetailService;
import com.next.service.TrainNumberService;
import com.next.service.TrainStationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/train/numberDetail")
public class TrainNumberDetailController {

    @Resource
    private TrainNumberDetailService trainNumberDetailService;
    @Resource
    private TrainStationService trainStationService;
    @Resource
    private TrainNumberService trainNumberService;

    @RequestMapping("/list.page")
    public ModelAndView page() {
        return new ModelAndView("trainNumberDetail");
    }

    @RequestMapping("/list.json")
    @ResponseBody
    public JsonData list() {
        List<TrainNumberDetail> detailList = trainNumberDetailService.getAll();

        List<TrainStation> stationList = trainStationService.getAll();
        Map<Integer, String> stationMap = stationList.stream().collect(Collectors.toMap(TrainStation::getId, TrainStation::getName));

        List<TrainNumber> numberList = trainNumberService.getAll();
        Map<Integer, String> numberMap = numberList.stream().collect(Collectors.toMap(TrainNumber::getId, TrainNumber::getName));

        List<TrainNumberDetailDto> dtoList = detailList.stream().map(detail -> {
            TrainNumberDetailDto dto = new TrainNumberDetailDto();
            dto.setId(detail.getId());
            dto.setFromStationId(detail.getFromStationId());
            dto.setToStationId(detail.getToStationId());
            dto.setFromStation(stationMap.get(detail.getFromStationId()));
            dto.setToStation(stationMap.get(detail.getToStationId()));
            dto.setFromCityId(detail.getFromCityId());
            dto.setToCityId(detail.getToCityId());
            dto.setTrainNumberId(detail.getTrainNumberId());
            dto.setTrainNumber(numberMap.get(detail.getTrainNumberId()));
            dto.setStationIndex(detail.getStationIndex());
            dto.setRelativeMinute(detail.getRelativeMinute());
            dto.setWaitMinute(detail.getWaitMinute());
            dto.setMoney(detail.getMoney());
            return dto;
        }).collect(Collectors.toList());
        return JsonData.success(dtoList);
    }

    @RequestMapping("/save.json")
    @ResponseBody
    public JsonData save(TrainNumberDetailParam param) {
        trainNumberDetailService.save(param);
        return JsonData.success();
    }

    @RequestMapping("/delete.json")
    @ResponseBody
    public JsonData delete(@RequestParam("id") Integer id) {
        trainNumberDetailService.delete(id);
        return JsonData.success();
    }
}
