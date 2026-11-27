package com.dorm.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class IinAndMaskingTest {
    @Test void validIinIsAccepted() {
        assertTrue(Iin.isValid("040517501237"));
        assertTrue(Iin.isValid("031102600458"));
        assertTrue(Iin.isValid("991231300013"));
    }

    @Test void wrongCheckDigitLengthOrCharactersAreRejected() {
        assertFalse(Iin.isValid("040517501238"));
        assertFalse(Iin.isValid("04051750123"));
        assertFalse(Iin.isValid("04051750123a"));
        assertFalse(Iin.isValid(null));
    }

    @Test void impossibleDatesAreRejected() {
        assertFalse(Iin.isValid("040231501230"));   // 31 February
        assertFalse(Iin.isValid("040517901237"));   // century digit 9 does not exist
    }

    @Test void birthDateAndGenderComeFromTheNumber() {
        assertEquals(LocalDate.of(2004, 5, 17), Iin.birthDate("040517501237").orElseThrow());
        assertEquals(LocalDate.of(1999, 12, 31), Iin.birthDate("991231300013").orElseThrow());
        assertEquals("MALE", Iin.gender("040517501237").orElseThrow());
        assertEquals("FEMALE", Iin.gender("031102600458").orElseThrow());
        assertTrue(Iin.birthDate("abc").isEmpty());
        assertTrue(Iin.gender("abc").isEmpty());
    }

    @Test void maskingKeepsOnlyTheLastFour() {
        assertEquals("********1237", Masking.mask("040517501237"));
        assertEquals("****", Masking.mask("12"));
        assertEquals("****", Masking.mask("1234"));
        assertNull(Masking.mask(null));
        assertEquals("", Masking.mask(""));
    }

    @Test void studentViewsHideWhatTheyShould() {
        Student s = new Student("S1", "Ayan", "Nur", LocalDate.of(2004, 5, 17), "MALE", "KZ", "040517501237",
                "046123456", "N12345678", "777", "a@b.kz", "IS", 2, "Street 1", "Mom", "888");
        assertEquals("Ayan Nur", s.name());
        assertEquals("********1237", s.masked().iin());
        assertEquals("777", s.masked().phone());
        assertNull(s.directoryView().iin());
        assertNull(s.directoryView().phone());
        assertEquals("IS", s.directoryView().faculty());
        assertEquals("Ayan", Student.basic("S2", "Ayan Nur", "1").firstName());
        assertEquals("Nur", Student.basic("S2", "Ayan Nur", "1").lastName());
    }
}
