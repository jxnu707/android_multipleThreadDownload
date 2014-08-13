import com.example.multipledownloader.db.DBOpenHelper;

import junit.framework.TestCase;


public class test extends TestCase {
	
	public void testExecutSQLscript(){
		assertEquals(true, DBOpenHelper.class);
	}
	
}
