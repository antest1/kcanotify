package com.antest1.kcanotify;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KcaUtilsTimeTest {
    @Test
    public void showsElapsedTimeBelowTheLimit() {
        assertEquals("00:15:00", KcaUtils.getElapsedTimeStr(15 * 60));
        assertEquals("99:59:59", KcaUtils.getElapsedTimeStr(KcaUtils.MAX_ELAPSED_TIME_SECOND));
    }

    @Test
    public void stopsAt99Hours59Minutes59Seconds() {
        assertEquals("99:59:59", KcaUtils.getElapsedTimeStr(100 * 3600));
        assertEquals("99:59:59", KcaUtils.getElapsedTimeStr(Integer.MAX_VALUE));
    }
}
