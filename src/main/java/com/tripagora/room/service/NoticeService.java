package com.tripagora.room.service;

import com.tripagora.auth.exception.AccessDeniedException;
import com.tripagora.room.dto.request.NoticeCreateRequest;
import com.tripagora.room.dto.request.NoticeUpdateRequest;
import com.tripagora.room.dto.response.NoticeListResponse;
import com.tripagora.room.dto.response.NoticeResponse;
import com.tripagora.room.dto.response.NoticeSummaryResponse;
import com.tripagora.room.entity.Notice;
import com.tripagora.room.entity.Room;
import com.tripagora.room.exception.NoticeNotFoundException;
import com.tripagora.room.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {
    private final RoomService roomService;
    private final NoticeRepository noticeRepository;

    @Transactional
    public NoticeResponse createNotice(long userId, long roomId, NoticeCreateRequest request) {
        Room room = roomService.checkRoomAndGuide(userId, roomId);

        Notice notice = Notice.builder()
                .room(room)
                .title(request.title())
                .content(request.content())
                .build();
        Notice savedNotice = noticeRepository.save(notice);

        return new NoticeResponse(
                savedNotice.getId(),
                savedNotice.getTitle(),
                savedNotice.getContent(),
                savedNotice.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public NoticeResponse getNotice(long userId, long roomId, long noticeId) {
        roomService.checkRoomAndParticipant(userId, roomId);
        Notice notice = checkNoticeAndRoom(noticeId, roomId);

        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public NoticeListResponse getNotices(long userId, long roomId, Pageable pageable) {
        Room room = roomService.checkRoomAndParticipant(userId, roomId);

        Slice<Notice> noticeSlice = noticeRepository.findByRoomOrderByIdDesc(room, pageable);

        List<NoticeSummaryResponse> summaries = noticeSlice.getContent().stream()
                .map(notice -> new NoticeSummaryResponse(
                        notice.getId(),
                        notice.getTitle(),
                        notice.getCreatedAt()
                ))
                .toList();

        return new NoticeListResponse(summaries, noticeSlice.hasNext());
    }

    @Transactional
    public NoticeResponse updateNotice(long userId, long roomId, long noticeId, NoticeUpdateRequest request) {
        roomService.checkRoomAndGuide(userId, roomId);
        Notice notice = checkNoticeAndRoom(noticeId, roomId);

        notice.update(request.title(), request.content());

        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt()
        );
    }

    @Transactional
    public void deleteNotice(long userId, long roomId, long noticeId) {
        roomService.checkRoomAndGuide(userId, roomId);
        Notice notice = checkNoticeAndRoom(noticeId, roomId);

        noticeRepository.delete(notice);
    }

    private Notice checkNoticeAndRoom(long noticeId, long roomId) {
        Notice notice = noticeRepository.findById(noticeId).orElseThrow(NoticeNotFoundException::new);

        if (!notice.getRoom().getId().equals(roomId)) {
            throw new AccessDeniedException();
        }

        return notice;
    }
}