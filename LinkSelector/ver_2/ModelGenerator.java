/******************************************************************************
 *
 ******************************************************************************/
import java.io.*; 

import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;

public class ModelGenerator {

    // -------------------------------------------------------------------------
    public ModelGenerator() { ;}

    public static void main(String args[]) throws Exception
    {
        try {
            if (args.length < 2) {
                System.out.println("Usage: java ModelGenerator <training file>" +
                                   " <model output file>");
                return;
            }

            CreateModel(args[0], args[1]);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return;
        }
    }

    // -------------------------------------------------------------------------
    public static void CreateModel(String trainingFile,
                                   String modelFile) throws Exception
    {
        System.out.println("Creating the model '" + modelFile + "' from " +
                           "training file '" + trainingFile + "'");
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

            Serialize(model, modelFile);

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

        System.out.println("Model created and serialized!");
    }

    // -------------------------------------------------------------------------
    public static void Serialize(DoccatModel model,
                                 String modelFile) throws Exception
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
}