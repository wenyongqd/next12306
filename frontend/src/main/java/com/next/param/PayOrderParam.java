package com.next.param;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@ToString
public class PayOrderParam {

    @NotBlank(message = "必须指定订单号")
    private String orderId;
}
