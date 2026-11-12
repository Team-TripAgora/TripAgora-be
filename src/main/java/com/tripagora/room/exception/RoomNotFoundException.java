package com.tripagora.room.exception;

import com.tripagora.common.code.RoomErrorCode;

public class RoomNotFoundException extends RoomException {
    public RoomNotFoundException() {
        super(RoomErrorCode.ROOM_NOT_FOUND);
    }
}