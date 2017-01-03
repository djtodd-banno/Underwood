package com.banno.annotations.example;


import com.banno.annotations.HelloWorld;

@HelloWorld(type = World.class, id = "B")
public class LandOfGators implements World {

    @Override
    public String getHello() {
        return  "Hey Ya\'ll";
    }
}
