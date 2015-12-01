package com.eigenmusik.services;

import com.eigenmusik.domain.Track;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "tracks", path = "tracks")
public interface TrackRepository extends PagingAndSortingRepository<Track, Long> {
    Page<Track> findByNameContains(@Param("name") String name, Pageable pageable);
}