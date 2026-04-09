package com.stream.app.videostream.playload;
import lombok.*;

import lombok.Builder;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomMessage {

    private String message;

    private boolean success = false;


}
