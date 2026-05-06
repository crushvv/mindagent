package com.mindagent.chat.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ModalInput(
        //统一化输入，type+content的格式
        @NotNull(message = "type is required") ModalityType type,
        @NotBlank(message = "content is required") String content
) {
}
