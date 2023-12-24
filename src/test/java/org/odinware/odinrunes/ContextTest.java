package org.odinware.odinrunes;

import static org.junit.Assert.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

public class ContextTest {

    private Context context;

    @BeforeEach
    public void setUp() {
        context = new Context();
    }

    @Test
    public void testAddCapturedData() {
        context.addCapturedData("Sample text", "Clipboard");
        List<Context.CapturedData> capturedDataList = context.getCapturedDataList();

        assertEquals(1, capturedDataList.size());

        Context.CapturedData capturedData = capturedDataList.get(0);
        assertEquals("Sample text", capturedData.getCapturedText());
        assertEquals("Clipboard", capturedData.getCaptureMethod());
    }

    @Test
    public void testDeleteCapturedData() {

        context.addCapturedData("Text 1", "Method 1");
        context.addCapturedData("Text 2", "Method 2");
        context.addCapturedData("Text 3", "Method 3");


        List<Context.CapturedData> capturedDataList = context.getCapturedDataList();

        assertEquals(3, capturedDataList.size());
        Context.CapturedData toBeDeleted = capturedDataList.get(0);
        Context.CapturedData notToBeDeleted = capturedDataList.get(1);

        context.deleteCapturedData(toBeDeleted);
        capturedDataList = context.getCapturedDataList();

        assertEquals(2, capturedDataList.size());
        assertFalse(capturedDataList.contains(toBeDeleted));
        assertEquals(true, capturedDataList.contains(notToBeDeleted));
    }


}
