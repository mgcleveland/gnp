/******************************************************************************
 *
 ******************************************************************************/
import java.sql.*;
import java.util.Date;


public class LinkSelector {
    
    private static final String user = "sqluser";
    private static final String password = "sqluserpw";

    // mysql database connection handle
    private static Connection lsConnection;
    
    private static FeedParser myParser;

    private static boolean outputFeedDescriptions;
    private static String feedDescriptionFile;

    // -------------------------------------------------------------------------
    public static void main (String[] args) throws Exception
    {
        if (args.length < 1) {
            System.out.println("Usage: java LinkSelector <model file name>" +
                               " (optional)<feeds detail output file>");
            return;
        }
        
        connectToDatabase();

        // setup description output variables
        if (args.length == 2) {
            outputFeedDescriptions = true;
            feedDescriptionFile = args[1];
        } else {
            outputFeedDescriptions = false;
            feedDescriptionFile = null;
        }
        
        myParser = new FeedParser(lsConnection, args[0]);

        processFeeds();

    }

    // -------------------------------------------------------------------------
    public static void processFeeds() throws Exception
    {
        String feedQuery = "select * from feeds";
        Statement query = lsConnection.createStatement();
        ResultSet result = query.executeQuery(feedQuery);
	
        while (result.next()) {
            if (outputFeedDescriptions) {
                myParser.GenerateDescriptions(result.getInt("FeedID"),
                                              result.getString("RSS_feed_link"),
                                              feedDescriptionFile);
            } else {
                myParser.ParseFeed(result.getInt("FeedID"),
                                   result.getString("RSS_feed_link"));
            }
        }

        result.close();
    }

    // -------------------------------------------------------------------------
    public static void connectToDatabase() throws Exception
    {
        // Connect to our MySQL DB
        try {
            Class.forName("com.mysql.jdbc.Driver");
            lsConnection = DriverManager.getConnection("jdbc:mysql://localhost/"
                                                       + "real_link_selection?"
                                                       + "user=" + user
                                                       + "&password=" +
                                                       password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

