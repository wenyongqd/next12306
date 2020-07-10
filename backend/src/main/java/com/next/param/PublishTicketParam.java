package com.next.param;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@ToString
public class PublishTicketParam {

    @NotBlank(message = "车次不可以为空")
    private String trainNumber;

    @NotBlank(message = "必须选中座位")
    private String trainSeatIds;
}
