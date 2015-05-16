/******************************************************************************
 *
 ******************************************************************************/
import java.sql.*;
import java.util.*;
import java.net.URL;
import java.net.InetAddress;
import java.net.UnknownHostException;
/*import java.util.Iterator;
  import java.util.StringTokenizer;*/
import java.io.*; 

// RSS parser
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import com.sun.syndication.io.ParsingFeedException;

// SAX parser
import org.xml.sax.SAXParseException;
import org.jdom.input.JDOMParseException;

// OpenNLP
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;

// JSoup library
import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
            System.out.println("Adding " + URL + " to descriptions file " +
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

                // sanity checks
                if (entry != null) {
                    if (entry.getDescription() != null) {
                        String desc = entry.getDescription().getValue();

                        // parse the text from the html
                        String storyText = ParseTextFromHTML(desc);

                        // parse the link to the main image from the html
                        String imageLink = ParseMainImageFromURL(entry.getLink());

                        // write to descriptions file, if specified
                        if (generateDescriptionFile) {
                            bw.write("Title: " + entry.getTitle());
                            bw.newLine();
                            bw.write("Description: " + storyText);
                            bw.newLine();
                        } else {
                            // check the story's "goodness"
                            if (IsGood(storyText)) {
                                //System.out.println("Adding: " + entry.getTitle());
                                AddLink(feedID, entry.getLink(),
                                        entry.getTitle(), imageLink);
                            } else {
                                //System.out.println("Filtering: " + entry.getTitle());
                            }
                        }
                    }
                }
            }
        } catch (UnknownHostException e) {
            System.out.println("Unknown host, URL: " + URL);
        } catch (FileNotFoundException e) {
            System.out.println("File not found, URL: " + URL);
        } catch (ParsingFeedException e) {
            System.out.println("Invalid feed, URL: " + URL);
        } catch (JDOMParseException e) {
            System.out.println("JDOM parse failed for feed, URL: " + URL);
        } catch (SAXParseException e) {
            System.out.println("SAX parse failed for feed, URL: " + URL);
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
    public String ParseTextFromHTML(String desc)
    {
        return Jsoup.parse(desc).text();
    }

    // -------------------------------------------------------------------------
    public String ParseMainImageFromURL(String URL) throws IOException
    {
        Document doc = Jsoup.connect(URL).get();
        Elements images = doc.select("[src]");
        int index = 0;
        String imageLink = null;
        List<Element> imageList = new ArrayList<Element>();

        for (Element img : images) {
            // add images to imageList for sorting
            if (img.tagName().equals("img")) {
                int w = 0;
                int h = 0;
                try {
                    w = Integer.parseInt(img.attr("width"));
                    h = Integer.parseInt(img.attr("height"));
                    imageList.add(img);
                } catch (NumberFormatException e) {
                    /*System.out.println("Height and width are not parseable for image: "+
                      img.attr("abs:src"));*/
                }
            }
        }

        // sort the collection by image size
        Collections.sort(imageList, new Comparator<Element>(){
                @Override public int compare(Element e1, Element e2) {
                    int e1Height = 0;
                    int e2Height = 0;
                    int e1Width = 0;
                    int e2Width = 0;
                    try {
                        e1Height = Integer.parseInt(e1.attr("height"));
                        e2Height = Integer.parseInt(e2.attr("height"));
                        e1Width = Integer.parseInt(e1.attr("width"));
                        e2Width = Integer.parseInt(e2.attr("width"));
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid image type found in list");
                    }

                    return ((e1Height > e2Height && e1Width > e2Width) ? 0 : 1);
                    //return s1.getFirstName().compareToIgnoreCase(s2.getFirstName());
                }
            });

        Element retVal = (Element)imageList.get(0);
        if (retVal != null) {
            imageLink = retVal.attr("abs:src");
        }

        return imageLink;
    }

    // -------------------------------------------------------------------------
    public void AddLink(int    FeedID,
                        String URL,
                        String headline,
                        String imageURL) throws SQLException
    {
        String insertQuery = "insert into links (FeedID, URL, headline, LinkImageURL) " +
            "values ('" + FeedID + "', '" + URL + "', '" + headline + "', '" + 
            imageURL + "')";
        Statement query = null;
        try {
            query = connection.createStatement();
            query.executeUpdate(insertQuery);
        } catch (SQLException e) {
            //e.printStackTrace();
            System.out.println(insertQuery);
        } finally {
            if (query != null) {
                System.out.println("Success");
                query.close();
            }
        }
    }
}
