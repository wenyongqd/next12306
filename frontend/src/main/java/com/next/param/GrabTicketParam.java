package com.next.param;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@ToString
public class GrabTicketParam {

    private int fromStationId;

    private int toStationId;

    @NotBlank(message = "日期不可以为空")
    @Length(max = 8, min = 8, message = "日期不合法")
    private String date; // yyyyMMdd

    @NotBlank(message = "车次不可以为空")
    private String number;

    @NotBlank(message = "必须选择乘车人")
    private String travellerIds;
}
