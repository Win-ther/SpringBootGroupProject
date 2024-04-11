package se.iths.springbootgroupproject.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MessageTest {

    private Message message = new Message();
    private User user = new User();
    private LocalDate date = LocalDate.now();

    @BeforeEach
    void setUp() {
        message = new Message();
        user = new User();
        date = LocalDate.now();
    }

    @Test
    @DisplayName("Two messages with same ID should be equal")
    void twoMessageWithSameIdShouldBeEqual() {
        message.setId(1L);
        Message message2 = new Message();
        message2.setId(1L);
        assertEquals(message, message2);

        message2.setId(2L);
        assertNotEquals(message, message2);
    }

    @Test
    @DisplayName("Hash code remains consistent for the same object instance")
    void hashCodeConsistencyForSameObject() {
        assertEquals(message.hashCode(), message.hashCode());
    }
}