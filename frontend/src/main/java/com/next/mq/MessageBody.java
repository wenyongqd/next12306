package com.next.mq;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MessageBody {

    private int topic;

    private int delay; // 毫秒

    private long sendTime = System.currentTimeMillis();

    private String detail;
}
