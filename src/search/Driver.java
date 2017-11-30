package search;

import index.*;
import query.BooleanQuery;
import query.QueryParser;

import java.io.*;
import java.util.List;
import java.util.Scanner;

/**
 * This class reads in the indices that were created in the "Invert" class and allows the user to search for terms with
 * Booleans within them
 *
 * @author hamza
 */
public class Driver {
    
    /**
     * The entry point of application.
     *
     * @param args the input arguments
     *
     * @throws IOException            an I/O exception has occurred
     * @throws ClassNotFoundException a class could not found in the folder
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        long start = System.nanoTime();
        
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(Invert.TERMS));
        TermIndex termIndex = (TermIndex) ois.readObject();
        
        ObjectInputStream ois2 = new ObjectInputStream(new FileInputStream(Invert.DOCS));
        DocumentIndex documentIndex = (DocumentIndex) ois2.readObject();
        
        ObjectInputStream ois3 = new ObjectInputStream(new FileInputStream(Invert.LIST));
        PostingLists postingLists = (PostingLists) ois3.readObject();
        
/*        ObjectInputStream ois4 = new ObjectInputStream(new FileInputStream(Invert.LOCATIONS));
        LocationsMap locationsMap = (LocationsMap) ois4.readObject();*/
        
        long end = System.nanoTime();
        long dif = end - start;
        System.out.println("dif = " + dif / 1_000_000_000.0);
        
        ois.close();
        ois2.close();
        ois3.close();
        //ois4.close();
        
        String queryTerms = getInput();
        
        while(!"Q".equals(queryTerms)) {
            start = System.nanoTime();
            
            BooleanQuery query = QueryParser.parse(queryTerms);
            PostingList postings = BooleanSearcher.search(query, termIndex, postingLists);
            
            end = System.nanoTime();
            dif = end - start;
            System.out.println("dif = " + dif / 1_000_000_000.0);
            
            if(postings == null) {
                System.out.println('\'' + queryTerms + "' isn't in any of the files or you have an error in your query.\n");
            }
            
            else {
                writeInfo(query, queryTerms, postings);
            }
            
            
            queryTerms = getInput();
        }
    }
    
    private static String getInput() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter a term to search for (Q to quit): ");
        return scanner.nextLine();
    }
    
    /**
     * This method writes the Postings into a table in a file that contain the word
     *
     * @param queryTerms the word to search for
     * @param postings   the list of Postings that is the final result of the query
     */
    private static void writeInfo(BooleanQuery query, String queryTerms, PostingList postings)
            throws FileNotFoundException {
        String queryTermsChanged = queryTerms.replace(" ", "_");
        boolean empty = query.getInputB().isEmpty();
        
        PrintWriter pw = new PrintWriter(queryTermsChanged + ".txt");
        pw.println("The word that was entered was '" + queryTerms + "'.");
        if("NEAR".equals(query.getOperator())) {
            pw.println("There are a total of " + postings.size() + " occurrences of these two words without "
                    + query.getK() + " words");
        }
        else {
            pw.println("There are a total of " + postings.size() + " documents that contain the word");
        }
        pw.println("Here are the documents that contain '" + queryTerms + "'.\n");
        queryTerms = queryTermsChanged;
        
        List<List<Integer>> locations = BooleanSearcher.getLocations();
        
        for(int i = 0; i < postings.size(); i++) {
            Posting posting = postings.get(i);
            pw.println("---------------");
            
            String name = posting.getName();
            pw.println("Filename: " + name);
            if(empty) {
                pw.println("Term frequency: " + posting.getFrequency());
            }
            
            StringBuilder sb = new StringBuilder();
            List<Integer> locs = locations.get(i);
            
            for(int j = 0; j < locs.size() - 1; j++) {
                Integer integer = locs.get(j);
                sb.append(integer).append(", ");
            }
            sb.append(locs.get(locs.size() - 1));
            
            if("NEAR".equals(query.getOperator())) {
                pw.println("Locations: " + sb);
            }
            else {
                pw.println("First locations: " + sb);
            }
            pw.println("---------------\n");
        }
        
        pw.close();
        
        System.out.println("Created " + queryTerms + ".txt");
    }
}
