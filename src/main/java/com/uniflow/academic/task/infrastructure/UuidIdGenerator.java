package com.uniflow.academic.task.infrastructure;

import com.uniflow.academic.task.application.ports.out.IdGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidIdGenerator implements IdGenerator {

    @Override
    public String newId() {
        return UUID.randomUUID().toString();
    }
}
