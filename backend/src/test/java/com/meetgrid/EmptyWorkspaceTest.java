package com.meetgrid;

import com.meetgrid.repository.MemberRepository;
import com.meetgrid.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:empty-workspace;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class EmptyWorkspaceTest {
    @Autowired MemberRepository members;
    @Autowired RoomRepository rooms;
    @Test void freshDatabaseDoesNotCreateSamplePeopleOrRooms() {
        assertThat(members.count()).isZero();
        assertThat(rooms.count()).isZero();
    }
}
