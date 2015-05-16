/******************************************************************************
 *
 ******************************************************************************/
import java.sql.*;
import java.net.URL;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.*; 

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;

public class FeedParser {

    private String modelFile;

    private Connection connection;
    
    // -------------------------------------------------------------------------
    public FeedParser(Connection dbConnection,
                      String     modelFileName) throws Exception
    {
        connection = dbConnection;
        modelFile = modelFileName;
    }

    // -------------------------------------------------------------------------
    public FeedParser() { ;}

    // -------------------------------------------------------------------------
    public void GenerateDescriptions(int    feedID,
                                     String URL,
                                     String descriptionFileName) throws Exception
    {
        EvaluateURL(feedID, URL, true, descriptionFileName);
    }


    // -------------------------------------------------------------------------
    public void ParseFeed(int     feedID,
                          String  URL) throws Exception
    {
        EvaluateURL(feedID, URL, false, null);
    }

    // -------------------------------------------------------------------------
    public void EvaluateURL(int     feedID,
                             String  URL,
                             boolean generateDescriptionFile,
                             String  descriptionFileName) throws Exception
    {
        // setup description writing structures, if needed
        File file = null;
        FileWriter fw = null;
        BufferedWriter bw = null;

        if (generateDescriptionFile) {
            System.out.println("Adding to descriptions file " +
                               descriptionFileName + "...");
            file = new File(descriptionFileName);
            if (!file.exists()) {
                file.createNewFile();
            }

            fw = new FileWriter(descriptionFileName, true);
            bw = new BufferedWriter(fw);
        }

        XmlReader reader = null;

        try {
            // load the RSS feed from the URL
            URL url  = new URL(URL);
            reader = new XmlReader(url);

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(reader);
	    
            // parse all the stories from this feed
            for (Iterator i = feed.getEntries().iterator(); i.hasNext();) {
                SyndEntry entry = (SyndEntry) i.next();

                String desc = entry.getDescription().getValue();
                int htmlIndex = desc.indexOf("<");
                desc = desc.substring(0, htmlIndex);

                // write to descriptions file, if specified
                if (generateDescriptionFile) {
                    bw.write("Title: " + entry.getTitle());
                    bw.newLine();
                    bw.write("Description: " + desc);
                    bw.newLine();
                } else {
                    // check the story's "goodness"
                    if (IsGood(desc)) {
                        System.out.println("Adding: " + entry.getTitle());
                        //AddLink(feedID, URL, entry.getTitle());
                    } else {
                        System.out.println("Filtering: " + entry.getTitle());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) { reader.close(); }
            if (bw != null)     { bw.close(); }
            if (fw != null)     { fw.close(); }
        }
    }

    // -------------------------------------------------------------------------
    public boolean IsGood(String story) throws Exception
    {
        boolean isGood = false;

        InputStream is = null;
        try {
            // create the openNLP document categorizer model
            is = new FileInputStream(modelFile);
            DoccatModel model = new DoccatModel(is);
            DocumentCategorizerME myCategorizer =
                new DocumentCategorizerME(model);
            
            // categorize the story
            double[] outcomes = myCategorizer.categorize(story);
            String category = myCategorizer.getBestCategory(outcomes);

            // if this is not good news, return false
            if (category.toLowerCase().equals("goodnews")) {
                isGood = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                is.close();
            }
        }

        return isGood;
    }

    // -------------------------------------------------------------------------
    public void AddLink(int    FeedID,
                        String URL,
                        String headline)
    {
        String insertQuery = "insert into links (FeedID, URL, headline) " +
            "values ('" + FeedID + "', '" + URL + "', '" + headline + "')";
        System.out.println(insertQuery);
    }
}
