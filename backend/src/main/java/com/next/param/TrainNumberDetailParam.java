package com.next.param;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
public class TrainNumberDetailParam {

    @NotNull(message = "车次不可以为空")
    @Min(1)
    private Integer trainNumberId;

    @NotNull(message = "出发站不可以为空")
    @Min(1)
    private Integer fromStationId;

    @NotNull(message = "到达站不可以为空")
    @Min(1)
    private Integer toStationId;

    @NotNull(message = "相对出发时间不可以为空")
    @Min(1)
    private Integer relativeMinute;

    @NotNull(message = "等待时间不可以为空")
    @Min(0)
    private Integer waitMinute;

    @NotBlank(message = "座位价钱不可以为空")
    private String money;

    @Min(0) // 代表该车次还有详情需要添加
    @Max(1) // 代表该车次详情全部添加完成
    private Integer end;
}
