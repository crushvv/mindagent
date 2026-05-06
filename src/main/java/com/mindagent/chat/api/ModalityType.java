package com.mindagent.chat.api;

public enum ModalityType {// enum是枚举类型，只能取其中的类型，否则反序列化会错误，防止前端输入不同，如img，Img等
    TEXT,
    AUDIO,
    IMAGE,
    VIDEO
}
