package com.next.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/admin/train/traveller")
public class TrainTravellerController {

    @RequestMapping("/list.page")
    public ModelAndView page() {
        return new ModelAndView("trainTraveller");
    }

}
