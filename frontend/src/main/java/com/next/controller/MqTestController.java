package com.next.controller;

import com.next.mq.MessageBody;
import com.next.mq.QueueTopic;
import com.next.mq.RabbitMqClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

@Controller
@RequestMapping("/mq")
public class MqTestController {

    @Resource
    private RabbitMqClient rabbitMqClient;

    @RequestMapping("/send.json")
    @ResponseBody
    public String send(@RequestParam("message") String message) {
        MessageBody messageBody = new MessageBody();
        messageBody.setTopic(QueueTopic.TEST);
        messageBody.setDetail(message);
        rabbitMqClient.send(messageBody);
        rabbitMqClient.sendDelay(messageBody, 5 * 1000);
        return "success";
    }
}
