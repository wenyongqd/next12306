package com.next.dto;

import com.next.model.TrainSeat;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class TrainSeatDto extends TrainSeat {

    @Getter
    @Setter
    private String trainNumber;

    @Getter
    @Setter
    private String fromStation;

    @Getter
    @Setter
    private String toStation;

    @Getter
    @Setter
    private String showStart;

    @Getter
    @Setter
    private String showEnd;
}
