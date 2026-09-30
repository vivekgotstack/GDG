package com.meetgrid;

import com.meetgrid.repository.MemberRepository;
import com.meetgrid.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EmptyWorkspaceTest extends PostgresTestSupport {
    @Autowired MemberRepository members;
    @Autowired RoomRepository rooms;
    @Test void freshDatabaseDoesNotCreateSamplePeopleOrRooms() {
        assertThat(members.count()).isZero();
        assertThat(rooms.count()).isZero();
    }
}
