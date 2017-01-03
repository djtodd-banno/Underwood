package com.banno.annotations.example;

import com.banno.annotations.HelloWorld;

@HelloWorld(type = World.class, id = "A")
public class LandOfTurtles implements World {

    @Override
    public String getHello() {
        return "Sup Dudes?";
    }
}
