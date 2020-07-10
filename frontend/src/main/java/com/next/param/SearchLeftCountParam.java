package com.next.param;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@ToString
public class SearchLeftCountParam {

    private int fromStationId;

    private int toStationId;

    @NotBlank(message = "日期不可以为空")
    @Length(max = 8, min = 8, message = "日期不合法")
    private String date; // yyyyMMdd
}
