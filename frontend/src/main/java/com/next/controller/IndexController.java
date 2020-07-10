package com.next.controller;

import com.next.common.JsonData;
import com.next.model.TrainUser;
import com.next.service.TrainStationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Controller
public class IndexController {

    @Resource
    private TrainStationService trainStationService;

    @RequestMapping("/")
    public ModelAndView index() {
        return new ModelAndView("index");
    }

    @RequestMapping("/mockLogin.json")
    @ResponseBody
    public JsonData mockLogin(HttpServletRequest request) {
        TrainUser trainUser = TrainUser.builder().id(1l).name("test").build();
        trainUser.setPassword(null);
        request.getSession().setAttribute("user", trainUser);
        return JsonData.success();
    }

    @RequestMapping("/logout.json")
    @ResponseBody
    public JsonData logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return JsonData.success();
    }

    @RequestMapping("/stationList.json")
    @ResponseBody
    public JsonData stationList() {
        return JsonData.success(trainStationService.getAll());
    }

    @RequestMapping("/info.json")
    @ResponseBody
    public JsonData info(HttpServletRequest request) {
        return JsonData.success(request.getSession().getAttribute("user"));
    }
}
