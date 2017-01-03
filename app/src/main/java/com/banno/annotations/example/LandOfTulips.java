package com.banno.annotations.example;


import com.banno.annotations.HelloWorld;

@HelloWorld(type = World.class, id = "D")
public class LandOfTulips implements World {

    @Override
    public String getHello() {
        return "Hallo Wereld";
    }
}
