package ch.epfl.bbp.uima.pubmed;

import gov.nih.nlm.ncbi.www.soap.eutils.EFetchPubmedServiceStub;
import gov.nih.nlm.ncbi.www.soap.eutils.EFetchPubmedServiceStub.PubmedArticleType;
import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceStub;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.axis2.AxisFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author renaud.richardet@epfl.ch
 */
public class PubmedSearch {
    private static Logger LOG = LoggerFactory.getLogger(PubmedSearch.class);

    private EUtilsServiceStub service;
    private EFetchPubmedServiceStub service2;

    public PubmedSearch() throws AxisFault {
	init();
    }

    private void init() throws AxisFault {
	service = new EUtilsServiceStub();
	service2 = new EFetchPubmedServiceStub();
    }

    /**
     * @param query
     * @return a list of articles
     */
    public List<PubmedArticleType> search(String query) throws RemoteException {
	return search(query, Integer.MAX_VALUE);
    }

    /**
     * @param query
     * @param maxNrResults
     * @return a list of articles
     */
    public List<Integer> searchIds(String query) throws RemoteException {

	EUtilsServiceStub.ESearchRequest req = new EUtilsServiceStub.ESearchRequest();
	req.setDb("pubmed");
	req.setEmail("gmail@gmail.com");
	req.setTerm(query);
	req.setRetStart("0");
	req.setRetMax(Integer.MAX_VALUE + "");
	EUtilsServiceStub.ESearchResult res = service.run_eSearch(req);
	int count = new Integer(res.getCount());
	LOG.debug("Found {} ids for query '{}'", count, query);

	List<Integer> articleIds = new ArrayList<Integer>();
	String[] idList = res.getIdList().getId();
	for (String id : idList) {
	    articleIds.add(new Integer(id));
	}
	assert (count == articleIds.size()) : "result counts should match, "
		+ articleIds.size() + ":" + count;
	return articleIds;
    }

    /**
     * @param query
     * @param maxNrResults
     * @return a list of articles
     */
    public List<PubmedArticleType> search(String query, int maxNrResults)
	    throws RemoteException {
	// STEP #1: search in PubMed for "query"
	EUtilsServiceStub.ESearchRequest req = new EUtilsServiceStub.ESearchRequest();
	req.setDb("pubmed");
	req.setTerm(query);
	req.setEmail("gmail@gmail.com");
	// not working req.setRetStart("0"); req.setRetMax(maxNrResults + "");
	// req.setSort("PublicationDate");
	req.setUsehistory("y");// important!
	EUtilsServiceStub.ESearchResult res = service.run_eSearch(req);
	int count = new Integer(res.getCount());
	LOG.debug("Found {} results for query '{}'", count, query);
	// results output
	String webEnv = res.getWebEnv();
	String query_key = res.getQueryKey();
	LOG.trace("WebEnv: " + webEnv + "\nQueryKey: " + query_key);

	// STEP #2: fetch articles from Pubmed
	List<PubmedArticleType> articles = new ArrayList<EFetchPubmedServiceStub.PubmedArticleType>();
	int fetchesPerRuns = Math.min(2000, maxNrResults);
	int runs = (int) Math.ceil(count / new Double(fetchesPerRuns));
	int start = 0;
	for (int i = 0; i < runs; i++) {
	    LOG.debug("Fetching results from id {} to id {} ", start, start
		    + fetchesPerRuns);
	    EFetchPubmedServiceStub.EFetchRequest req2 = new EFetchPubmedServiceStub.EFetchRequest();
	    req2.setWebEnv(webEnv);
	    req2.setQuery_key(query_key);
	    req2.setRetstart(start + "");
	    req2.setRetmax(fetchesPerRuns + "");

	    EFetchPubmedServiceStub.EFetchResult res2 = service2
		    .run_eFetch(req2);
	    for (int j = 0; j < res2.getPubmedArticleSet()
		    .getPubmedArticleSetChoice().length; j++) {

		PubmedArticleType art = res2.getPubmedArticleSet()
			.getPubmedArticleSetChoice()[j].getPubmedArticle();
		if (art != null) {
		    LOG.trace("found ID{}:{}", art.getMedlineCitation()
			    .getPMID(), art.getMedlineCitation().getArticle()
			    .getArticleTitle());
		    articles.add(art);
		    if (articles.size() == maxNrResults) { // enough!
			return articles;
		    }
		}
	    }
	    start += fetchesPerRuns;
	}
	return articles;
    }
}