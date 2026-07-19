// RoomResourceCreator.java
package com.campus.booking.factory;
import com.campus.booking.model.Resource;

public class RoomResourceCreator extends ResourceCreator {
    @Override
    public Resource createResource(String name) {
        Resource r = new Resource();
        r.setName(name);
        r.setType("ROOM");
        return r;
    }
}