package com.tripagora.participation.dto.response;

public record ParticipationResponse(Long participationId,
                                    Long tripId,
                                    Integer currentParticipants) {}