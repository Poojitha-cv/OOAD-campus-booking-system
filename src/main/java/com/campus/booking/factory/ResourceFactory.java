package com.campus.booking.factory;

import com.campus.booking.model.Resource;

/**
 * Factory Method Pattern — Client
 * Delegates resource creation to the appropriate concrete creator.
 */
public class ResourceFactory {

    public static Resource createResource(String name, String type) {
        ResourceCreator creator = getCreator(type.toUpperCase());
        return creator.createResource(name);
    }

    private static ResourceCreator getCreator(String type) {
        switch (type) {
            case "LAB":         return new LabResourceCreator();
            case "ROOM":        return new RoomResourceCreator();
            case "AUDITORIUM":  return new AuditoriumResourceCreator();
            default: throw new IllegalArgumentException("Unknown resource type: " + type);
        }
    }
}