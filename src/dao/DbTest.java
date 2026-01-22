package dao;

public class DbTest {
    public static void main(String[] args) throws Exception {
        DB.connect().close();
        System.out.println("SQLite connected successfully");
    }
}
