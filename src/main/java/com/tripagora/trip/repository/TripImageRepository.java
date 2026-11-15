package com.tripagora.trip.repository;

import com.tripagora.trip.entity.TripImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TripImageRepository extends JpaRepository<TripImage, Long> {

    // 주어진 사진 주소 중 여행 사본이 아직 쓰고 있는 주소
    @Query("SELECT DISTINCT ti.imageUrl FROM TripImage ti WHERE ti.imageUrl IN :imageUrls")
    List<String> findImageUrlsIn(@Param("imageUrls") Collection<String> imageUrls);
}
