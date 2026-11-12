package com.tripagora.session.repository;

import com.tripagora.session.dto.request.SessionSearchRequest;
import com.tripagora.session.entity.Session;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface SessionRepositoryCustom {
    Slice<Session> search(SessionSearchRequest request, Pageable pageable);
}