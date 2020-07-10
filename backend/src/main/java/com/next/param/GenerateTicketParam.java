package com.next.param;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
public class GenerateTicketParam {

    @NotNull(message = "车次不可以为空")
    private Integer trainNumberId;

    @NotBlank(message = "必须有发车时间")
    private String fromTime;
}
