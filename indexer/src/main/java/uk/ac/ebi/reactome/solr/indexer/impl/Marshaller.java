package uk.ac.ebi.reactome.solr.indexer.impl;

import uk.ac.ebi.reactome.solr.indexer.exception.IndexerException;
import uk.ac.ebi.reactome.solr.indexer.model.IndexDocument;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by flo on 5/28/14.
 */
public class Marshaller {

    public static final String INDENT = "  ";
    public static final String NEW_LINE = "\n";

    private final String name;
    private final String description;
    private final String release;

    private Writer writer;


    public Marshaller( String fileName, String name, String description, String release ) {
        File output = new File(fileName); // changed to file then check below makes sense

        this.name = name;
        this.description = description;
        this.release = release;


        if ( output == null ) {
            throw new IllegalArgumentException( "output file must not be null." );
        }

        if ( !output.exists() ) {
            try {
                output.createNewFile();
            } catch ( IOException e ) {
                throw new IllegalArgumentException( "Could not create a new output file.", e );
            }
        }

        if ( !output.canWrite() ) {
            throw new IllegalArgumentException( "Cannot write on " + output.getAbsolutePath() );
        }

        try {
            this.writer = new BufferedWriter(new FileWriter(output));
        } catch (IOException e) {
            throw new IllegalArgumentException( "Cannot write on " + output.getAbsolutePath(), e );
        }

    }


    public void writeHeader() throws IndexerException {
        try {
            writer.write( "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" + NEW_LINE );
            writer.write("<database>" + NEW_LINE);
            writer.write( INDENT + "<name>" + name + "</name>" + NEW_LINE );
            writer.write( INDENT + "<description>" + description + "</description>" + NEW_LINE );
            writer.write( INDENT + "<release>" + release + "</release>" + NEW_LINE );
            writer.write( INDENT + "<release_date>" + getCurrentDate() + "</release_date>" + NEW_LINE );
            writer.write( INDENT + "<entries>" + NEW_LINE );
        } catch ( IOException e ) {
            throw new IndexerException( e );
        }
    }
    public void writeEntry(IndexDocument document) throws IndexerException {
        final String i = INDENT + INDENT;
        final String ii = INDENT + INDENT+ INDENT;
        final String iii = INDENT + INDENT + INDENT + INDENT;

        try {
        writer.write( i + "<entry id=\"" + document.getDbId() + "\">" + NEW_LINE );
        writer.write( ii + "<name>" + document.getName() + "</name>" + NEW_LINE );
        if (document.getSummation()!= null) {
            writer.write( ii +"<description>" + document.getSummation() + "</description>" + NEW_LINE );
        }
//            Authors
//            Keywords
//        writer.write( ii + "<dates>" + NEW_LINE );
//            writeCreationDate( writer, document.getCreated(), iii );
//            writeLastUpdateDate( writer, document.getUpdated(), iii );
//        writer.write( ii + "</dates>" + NEW_LINE );



//        if (document.getCrossReferences()!=null ) {
//            writer.write( ii + "<cross_references>" + NEW_LINE );
//
//           for(db name KEY document) {
//                goTerms;
//            pubmed;
//            Compartments;



//              writeRef(
//           }
//
//            writer.write( ii + "</cross_references>" + NEW_LINE );
//        }


        writer.write( ii + "<additional_fields>" + NEW_LINE );
            writeField("Species" ,document.getSpecies(),iii);
            writeField("Type" ,document.getType(),iii);
            writeField("stId" ,document.getType(),iii);
            writeField("synonyms" ,document.getType(),iii);
            writeField("compartmentName" ,document.getType(),iii);
            writeField("compartmentAccession" ,document.getType(),iii);
            writeField("goBiologicalProcessName" ,document.getType(),iii);
            writeField("goBiologicalProcessAccession" ,document.getType(),iii);




        writer.write( ii + "</additional_fields>" + NEW_LINE );

        writer.write( i + "</entry>" + NEW_LINE );
    } catch ( IOException e ) {
        throw new IndexerException( e );
    }
    }

    public void writeFooter(int entriesCount) throws IndexerException {
        try {
            writer.write( INDENT + "</entries>" + NEW_LINE );
            writer.write( INDENT + "<entry_count>" + entriesCount + "</entry_count>" + NEW_LINE );
            writer.write("</database>" + NEW_LINE);
            closeIndex();
        } catch ( IOException e ) {
            throw new IndexerException( e );
        }
    }


    private void closeIndex() throws IOException {
        writer.flush();
        writer.close();
    }


    private void writeRef( String db, String id, String indent ) throws IOException {
        db = db.replaceAll( "/", "_" );
        writer.write(indent + "<ref dbname=\"" + db + "\" dbkey=\"" + id + "\" />" + NEW_LINE);
    }

    private void writeField( String name, String text, String indent ) throws IOException {
        if (text!= null) {

            writer.write( indent + "<field name=\"" + name + "\">" + text + "</field>" + NEW_LINE );
        }

    }


    public String getCurrentDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        return(dateFormat.format(date));
    }
}


