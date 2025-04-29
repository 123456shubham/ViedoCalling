package com.example.agroaviedocalling.media;

public interface PackableEx extends Packable {
    void unmarshal(ByteBuf in);
}
