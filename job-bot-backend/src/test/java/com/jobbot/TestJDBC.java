import java.sql.*;
import java.util.Properties;

public class TestJDBC {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://aws-0-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require";
        Properties props = new Properties();
        props.setProperty("user", "postgres.cdigwphgihlaqylsrexh");
        props.setProperty("password", "Jobpilot@123");

        try (Connection conn = DriverManager.getConnection(url, props)) {
            // Pick one job that was filtered out and force it to APPROVED
            try (Statement stmt = conn.createStatement()) {
                int updated = stmt.executeUpdate("UPDATE job_queue SET status = 'APPROVED' WHERE status = 'FILTERED_OUT' AND platform = 'NAUKRI'");
                System.out.println("Updated " + updated + " jobs to APPROVED");
            }
        }
    }
}
