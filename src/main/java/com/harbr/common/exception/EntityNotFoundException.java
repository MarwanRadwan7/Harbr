package com.harbr.common.exception;

public class EntityNotFoundException extends BusinessException {

    public EntityNotFoundException(String entity, Object id) {
        super("ENTITY_NOT_FOUND", entity + " not found with id: " + id);
    }

    public EntityNotFoundException(String message) {
        super("ENTITY_NOT_FOUND", message);
    }
}