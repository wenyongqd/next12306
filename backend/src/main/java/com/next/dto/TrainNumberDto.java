package com.next.dto;

import com.next.model.TrainNumber;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class TrainNumberDto extends TrainNumber{

    @Getter
    @Setter
    private String fromStation;

    @Getter
    @Setter
    private String toStation;
}
