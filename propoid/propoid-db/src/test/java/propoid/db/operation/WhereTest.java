package propoid.db.operation;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import propoid.core.Property;
import propoid.core.Propoid;
import propoid.db.Where;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class WhereTest {

	@Test
	public void testWhere() throws Exception {
		Foo foo = new Foo();

		Where.greaterEqual(foo.stringP, "A");
	}
}
