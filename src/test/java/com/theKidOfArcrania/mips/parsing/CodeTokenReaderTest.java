package com.theKidOfArcrania.mips.parsing;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

@SuppressWarnings("JavaDoc")
public class CodeTokenReaderTest
{


    private CodeTokenReader initReader(String code)
    {
        CodeTokenReader reader = new CodeTokenReader(code);
        reader.nextLine();
        reader.addErrorLogger(new ErrorLogger()
        {
            @Override
            public void logError(String description, Range highlight)
            {
                fail(description);
            }

            @Override
            public void logWarning(String description, Range highlight)
            {
                fail(description);
            }
        });
        return reader;
    }

    @Test(expected=IllegalStateException.class)
    public void testNoLine()
    {
        CodeTokenReader reader = new CodeTokenReader("Hello this is identifiers #comment here");
        reader.nextToken();
    }

    @Test
    public void testIdentifierParsing()
    {
        CodeTokenReader reader = initReader("Hello this is identifiers #comment here");
        int count = 0;
        while (reader.nextToken())
            count++;
        assertEquals(count, 4);
    }


    @Test
    public void testResetLine() throws Exception
    {
        CodeTokenReader reader = initReader("Hello this is identifiers #comment here");
        while (reader.nextToken());
        reader.resetLine();
        reader.nextToken();
        assertEquals("Hello",reader.getToken());
    }

    @Test
    public void testGetTokenNum() throws Exception
    {
        CodeTokenReader reader = initReader("A BB C D #comment here");
        int count = 0;
        while (reader.nextToken())
            assertEquals(count++, reader.getTokenNum());
    }

    @Test
    public void testGetTokensRead() throws Exception
    {
        CodeTokenReader reader = initReader("A BB C D #comment here");
        while (reader.nextToken());
        assertEquals(4, reader.getTokensRead());
    }

    @Test
    public void testTokenRange() throws Exception
    {
        CodeTokenReader reader = initReader("A BB C D #comment here");
        reader.nextToken();
        reader.nextToken();
        assertEquals(2, reader.getTokenStartIndex());
        assertEquals(4, reader.getTokenEndIndex());
    }

    @Test
    public void testNextArgument() throws Exception
    {
        CodeTokenReader reader = initReader("A, BB, C, D #comment here");
        int count = 0;
        while (reader.nextArgument())
            count++;
        assertEquals(4, count);
    }

    @Test(expected = IllegalStateException.class)
    public void testNextArgumentDoubleComma() throws Exception
    {
        CodeTokenReader reader = new CodeTokenReader("A, , BB, C, D, #comment here");
        reader.nextLine();
        reader.addErrorLogger(new ErrorLogger()
        {
            @Override
            public void logError(String description, Range highlight)
            {
                assertEquals("Unexpected comma.", description);
                throw new IllegalStateException();
            }

            @Override
            public void logWarning(String description, Range highlight)
            {
                fail(description);
            }
        });
        int count = 0;
        while (reader.nextArgument())
            count++;
        assertEquals(4, count);
    }

    @Test(expected = IllegalStateException.class)
    public void testNextArgumentTrailingComma() throws Exception
    {
        CodeTokenReader reader = new CodeTokenReader("A, BB, C, D, #comment here");
        reader.nextLine();
        reader.addErrorLogger(new ErrorLogger()
        {
            @Override
            public void logError(String description, Range highlight)
            {
                assertEquals("Unexpected comma.", description);
                throw new IllegalStateException();
            }

            @Override
            public void logWarning(String description, Range highlight)
            {
                fail(description);
            }
        });
        int count = 0;
        while (reader.nextArgument())
            count++;
        assertEquals(4, count);
    }

    @Test
    public void testVisitToken() throws Exception
    {
        ArrayList<String> tokens = new ArrayList<>();
        CodeTokenReader reader = initReader("A BB C D #comment here");
        while (reader.nextToken())
            tokens.add(reader.getToken());
        assertEquals(4, tokens.size());

        reader.visitToken(0);
        reader.error("Should not be an error.", new Range(0, 0, 0, 0));

        int ind = 0;
        do
            assertEquals(tokens.get(ind++), reader.getToken());
        while (reader.nextToken());
        assertEquals(4, tokens.size());

        while (ind --> 0)
        {
            reader.visitToken(ind);
            assertEquals(tokens.get(ind), reader.getToken());
        }
    }
}
