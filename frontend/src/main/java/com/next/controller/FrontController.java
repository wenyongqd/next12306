package com.next.controller;

import com.next.common.JsonData;
import com.next.common.RequestHolder;
import com.next.dto.TrainNumberLeftDto;
import com.next.model.TrainUser;
import com.next.param.CancelOrderParam;
import com.next.param.GrabTicketParam;
import com.next.param.PayOrderParam;
import com.next.param.SearchLeftCountParam;
import com.next.service.TrainOrderService;
import com.next.service.TrainSeatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

@Controller
@RequestMapping("/front")
@Slf4j
public class FrontController {

    @Resource
    private TrainSeatService trainSeatService;
    @Resource
    private TrainOrderService trainOrderService;

    @RequestMapping("/searchLeftCount.json")
    @ResponseBody
    public JsonData searchLeftCount(SearchLeftCountParam param) {
        try {
            List<TrainNumberLeftDto> dtoList = trainSeatService.searchLeftCount(param);
            return JsonData.success(dtoList);
        } catch (Exception e) {
            log.error("searchLeftCount exception, param:{}", param, e);
            return JsonData.fail("查询异常，请稍后尝试");
        }
    }

    @RequestMapping("/grab.json")
    @ResponseBody
    public JsonData grabTicket(GrabTicketParam param) {
        TrainUser trainUser = RequestHolder.getCurrentUser();
        return JsonData.success(trainSeatService.grabTicket(param, trainUser));
    }

    @RequestMapping("/mockPay.json")
    @ResponseBody
    public JsonData payOrder(PayOrderParam param) {
        TrainUser trainUser = RequestHolder.getCurrentUser();
        trainOrderService.payOrder(param, trainUser.getId());
        return JsonData.success();
    }

    @RequestMapping("/mockCancel.json")
    @ResponseBody
    public JsonData cancelOrder(CancelOrderParam param) {
        TrainUser trainUser = RequestHolder.getCurrentUser();
        trainOrderService.cancelOrder(param, trainUser.getId());
        return JsonData.success();
    }

}
