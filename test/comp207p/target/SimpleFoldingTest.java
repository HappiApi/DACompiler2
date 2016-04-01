package comp207p.target;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import static org.junit.Assert.assertEquals;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * test simple folding
 */
public class SimpleFoldingTest {

    SimpleFolding sf = new SimpleFolding();

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void cleanUpStreams() {
        System.setOut(null);
    }

    @Test
    public void testLdcAdd(){
        sf.ldcAdd();
        assertEquals("12412\n", outContent.toString());
    }

    @Test
    public void testLdcNeg(){
        sf.ldcNeg();
        assertEquals("67\n", outContent.toString());
    }

    @Test
    public void testLdcSub(){
        sf.ldcSub();
        assertEquals("-12278\n", outContent.toString());
    }

    @Test
    public void testLdcNestedAdd(){
        sf.ldcNestedAdd();
        assertEquals("12444\n", outContent.toString());
    }

    @Test
    public void testIconstMul(){
        sf.iconstMul();
        assertEquals("6\n", outContent.toString());
    }

    @Test
    public void testCombinationMulDiv(){
        sf.combinationMulDiv();
        assertEquals("90000\n", outContent.toString());
    }

    @Test
    public void testFAdd(){
        sf.fAdd();
        assertEquals("3.0\n", outContent.toString());
    }

    @Test
    public void testLSub(){
        sf.lSub();
        assertEquals("19999\n", outContent.toString());
    }

    @Test
    public void testDDiv(){
        sf.dDiv();
        assertEquals("4.5\n", outContent.toString());
    }

}
