package io.sapl.server.ce.model.sapldocument;

import java.io.Serializable;
import java.util.Collection;

import lombok.NonNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Interface for a repository for accessing persisted
 * {@link PublishedSaplDocument}.
 */
public interface PublishedSaplDocumentRepository extends CrudRepository<PublishedSaplDocument, Long>, Serializable {
	@Override
	@NonNull
	Collection<PublishedSaplDocument> findAll();

	@Query(value = "SELECT s FROM PublishedSaplDocument s WHERE s.documentName = :documentName")
	Collection<PublishedSaplDocument> findByDocumentName(@Param(value = "documentName") String documentName);
}
