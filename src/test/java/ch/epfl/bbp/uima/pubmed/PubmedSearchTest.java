package ch.epfl.bbp.uima.pubmed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import gov.nih.nlm.ncbi.www.soap.eutils.EFetchPubmedServiceStub.PubmedArticleType;

import java.util.List;

import org.junit.Test;

public class PubmedSearchTest {

    @Test
    public void testNeuron() throws Exception {
	List<PubmedArticleType> articles = new PubmedSearch().search("axon",
		5);
	assertEquals(5, articles.size());
    }
    @Test
    public void testNeuron2() throws Exception {
	List<PubmedArticleType> articles = new PubmedSearch().search("axon",
		100);
	assertEquals(100, articles.size());
    }

   

    @Test
    public void testMesh() throws Exception {
	List<Integer> articles = new PubmedSearch().searchIds("Axons[mesh]");
	assertTrue(articles.size() > 10000);
    }
}
