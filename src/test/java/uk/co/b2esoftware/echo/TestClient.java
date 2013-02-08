package uk.co.b2esoftware.echo;

import org.junit.Test;
import uk.co.b2esoftware.time.TimeClient;

/**
 * User: art
 * Date: 16/01/2013
 * Time: 14:03
 */
public class TestClient
{
    @Test
    public void testClient()
    {
        TimeClient client = new TimeClient("localhost", 9797);
        client.run();


    }
}
