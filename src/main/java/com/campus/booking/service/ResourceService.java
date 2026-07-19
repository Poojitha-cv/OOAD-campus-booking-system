package com.campus.booking.service;

import com.campus.booking.factory.ResourceFactory;
import com.campus.booking.model.Resource;
import com.campus.booking.repository.ResourceRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceService {

    @Autowired
    private ResourceRepository resourceRepository;

    public void addResource(String name, String type) {

        Resource resource = ResourceFactory.createResource(name, type);
        resourceRepository.save(resource);
    }

    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    public List<Resource> searchByType(String type) {
        return resourceRepository.findByType(type);
    }
    public void deleteResource(Long id) {
    resourceRepository.deleteById(id);
}
}