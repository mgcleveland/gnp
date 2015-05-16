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
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;

public class FeedParser {

    //private static final String trainingFile = "gnp_training_file.en";
    private String modelFile;// = "training_model";

    private Connection connection;
    
    // -------------------------------------------------------------------------
    public FeedParser(Connection dbConnection,
                      String     modelFileName) throws Exception
    {
        connection = dbConnection;
        modelFile = modelFileName;
        /*try {
            CreateModel();
        } catch (Exception e) {
            e.printStackTrace();
            }*/
    }

    // -------------------------------------------------------------------------
    public FeedParser() { ;}

    
    // -------------------------------------------------------------------------
    public void ParseFeed(int    feedID,
                          String URL) throws Exception
    {
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

                // check the story's "goodness"
                if (IsGood(desc)) {
                    System.out.println("Adding: " + entry.getTitle());
                    //AddLink(feedID, URL, entry.getTitle());
                } else {
                    System.out.println("Filtering: " + entry.getTitle());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    // -------------------------------------------------------------------------
    /*public void CreateModel() throws Exception
    {
        System.out.println("Creating the model...");
        InputStream is = null;
        try {
            // open the model file for document classification
            File file = new File(trainingFile);
            is = new FileInputStream(file);
            
            // create the openNLP document categorizer model
            ObjectStream<String> lineStream =
                new PlainTextByLineStream(is, "UTF-8");
            ObjectStream<DocumentSample> sampleStream
                = new DocumentSampleStream(lineStream);
            DoccatModel model = DocumentCategorizerME.train("en", sampleStream);

            Serialize(model);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Model serialized!");
    }

    // -------------------------------------------------------------------------
    public void Serialize(DoccatModel model) throws Exception
    {
        OutputStream modelOut = null;
        try {
            modelOut = 
                new BufferedOutputStream(new FileOutputStream(modelFile));
            model.serialize(modelOut);
        }
        catch (IOException e) {
            // Failed to save model
            e.printStackTrace();
        }
        finally {
            if (modelOut != null) {
                modelOut.close();
            }
        }
    }
    */
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
            System.out.println("Category: " + category);

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


/*
    // -------------------------------------------------------------------------
    public boolean DeprecatedIsGood(String story) throws Exception
    {
        String queryTracker = "";
        boolean isGood = true;

        try {
            StringTokenizer tokenizer = new StringTokenizer(story);
            Statement stmt = connection.createStatement();

            while (tokenizer.hasMoreElements()) {

                // extract and format word
                String token = (String)tokenizer.nextElement();
                token = token.toLowerCase();
                token = token.replaceAll("[^A-Za-z]", "");
                
                // check for banned words
                if (IsBannedWord(token, stmt)) {
                    isGood = false;
                }
            }

        } catch (Exception e) {
            System.out.println(queryTracker);
        } finally {
            return isGood;
        }
    }
        
    // -------------------------------------------------------------------------
    public boolean IsBannedWord(String word,
                                Statement stmt) throws Exception
    {
        boolean isBad = false;
        String queryTracker = "";
        try {
            // check for banned words
            String query = "select * from word_table where word like '" +
                word + "' and banned_word=1";
            queryTracker = query;
            ResultSet result = stmt.executeQuery(query);

            // this was a banned word, filter this story
            result.last();
            if (result.getRow() > 0) {
                isBad = true;
            }
            
            result.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return isBad;
        }
    }
}    

*/