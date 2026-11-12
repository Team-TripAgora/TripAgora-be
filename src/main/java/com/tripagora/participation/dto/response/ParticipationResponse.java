package com.tripagora.participation.dto.response;

public record ParticipationResponse(Long participationId,
                                    Long sessionId,
                                    Integer currentParticipants) {}