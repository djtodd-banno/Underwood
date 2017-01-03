package com.banno.annotations.example;

import com.banno.annotations.HelloWorld;

@HelloWorld(type = World.class, id = "C")
public class LandOfBeer implements World {

    @Override
    public String getHello() {
        return "Hallo Welt";
    }
}
