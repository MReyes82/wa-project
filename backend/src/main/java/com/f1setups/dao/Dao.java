package com.f1setups.dao;

import java.util.List;
import java.util.Optional;
import java.util.Map;

/*
    * Dao interface that defines an abstract API that performs
    * CRUD operations on objects of type T.
 */
public interface Dao<T>
{
    /**
     * READ default operation: get by id method definition
     * @param id long number retrieved from the data layer
     * @return An optional object for whether the retrieval
     * is successful or failed.
    */
    Optional<T> get(long id);

    /**
     * READ custom operation, to be used by the desired Service
     * as a method to retrieve an entity from the database with a custom parameter
     * (different from ID)
     * @param field column defined in the table of the entity
     * @param value actual value to be used
     * @return An optional object for whether the retrieval
     * is successful or failed.
     */
    Optional<T> getBy(String field, String value);

    List<T> getAll();

    /**
     * POST operation
     * @param entity
     * @return Optional of value <T> to return after creation.
     */
    Optional<T> save(T entity);

    /**
     * UPDATE/PUT operation: Full resource replacement
     * @param entity Complete entity with all fields to replace
     * @return Optional of the updated entity, or empty if not found/failed
     */
    Optional<T> updateFull(T entity);

    /**
     * UPDATE/PATCH operation: Partial resource update
     * @param id ID of the entity to update
     * @param fields Map of field names to new values (only these fields are updated)
     * @return boolean true if update succeeded, false otherwise
     */
    boolean updatePartial(long id, Map<String, Object> fields);

    /**
     * DELETE operation: remove the entity from the database
     * @param entity element to be removed
     */
    void delete(T entity);
}
