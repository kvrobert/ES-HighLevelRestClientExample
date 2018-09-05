package com.example.highrestclienttest.service;

import com.example.highrestclienttest.Exceptions.MCFAuthorizerException;
import com.example.highrestclienttest.configs.MCFConfigurationParameters;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Service
public class MCFAuthService {

    @Autowired
    MCFConfigurationParameters mcfConfigurationParameters;

    private static final Logger LOG = LoggerFactory.getLogger(MCFAuthService.class);
    private static final String NOSECURITY_TOKEN = "__nosecurity__";

    private String authorityBaseURL;
    private String fieldAllowDocument;
    private String fieldDenyDocument;
    private String fieldAllowParent;
    private String fieldDenyParent;
    private String fieldAllowShare;
    private String fieldDenyShare;



    public BoolQueryBuilder getAuthFilter(String userDomain) throws IOException {
        initConfigParameters();
        return (BoolQueryBuilder) buildAuthorizationFilter(userDomain);
    }

    public void initConfigParameters(){
        authorityBaseURL = mcfConfigurationParameters.getAuthorityServiceBaseURL();
        fieldAllowDocument = mcfConfigurationParameters.getAllowFieldPrefix() + "document";
        fieldDenyDocument = mcfConfigurationParameters.getDenyFieldPrefix() + "document";
        fieldAllowShare = mcfConfigurationParameters.getAllowFieldPrefix() + "share";
        fieldDenyShare = mcfConfigurationParameters.getDenyFieldPrefix() + "share";
        fieldAllowParent = mcfConfigurationParameters.getAllowFieldPrefix() + "parent";
        fieldDenyParent = mcfConfigurationParameters.getDenyFieldPrefix() + "parent";
    }

    public List<String> getAllowsTokens(String domain) throws IOException {
        authorityBaseURL = mcfConfigurationParameters.getAuthorityServiceBaseURL();
        StringBuilder urlBuffer = new StringBuilder(authorityBaseURL);
        urlBuffer.append("/UserACLs");

        // ha az érkezett domain nev > 0, akkor hibás legyen, csak 1 userrel dolgozunk

        urlBuffer.append("?");
        urlBuffer.append("username=");
        // ha a domain nem szabványos email cím...hiba
        urlBuffer.append(domain);


        URL authURL = new URL(urlBuffer.toString());
        HttpURLConnection connection = (HttpURLConnection) authURL.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("content-Type", "text/html");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        //        connection.setInstanceFollowRedirects(false);

        List<String> tokenList = new ArrayList<>();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {

            if (connection.getResponseCode() == 200) {
                while (true) {
                    String line = in.readLine();
                    if (line == null)
                        break;
                    if (line.startsWith("TOKEN:")) {
                        tokenList.add(line.substring("TOKEN:".length()));
                    } else {
                        LOG.info("Authory response..." + line);
                    }
                }
            } else {
                LOG.warn("Authentication error.");
                throw new MCFAuthorizerException("Authentication error.");
            }


        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            throw new MCFAuthorizerException(ex.getMessage());
        }
        return tokenList;
    }


    /** Main method for building a filter representing appropriate security.
     **@return the wrapped query enforcing ManifoldCF security.
     */
    public QueryBuilder buildAuthorizationFilter(String userDomain)
            throws MCFAuthorizerException, IOException {

        BoolQueryBuilder bq = new BoolQueryBuilder();
        List<String> userAccessTokens = getAllowsTokens(userDomain);

        QueryBuilder allowShareOpen = new TermQueryBuilder(fieldAllowShare,NOSECURITY_TOKEN);
        QueryBuilder denyShareOpen = new TermQueryBuilder(fieldDenyShare,NOSECURITY_TOKEN);
        QueryBuilder allowParentOpen = new TermQueryBuilder(fieldAllowParent,NOSECURITY_TOKEN);
        QueryBuilder denyParentOpen = new TermQueryBuilder(fieldDenyParent,NOSECURITY_TOKEN);
        QueryBuilder allowDocumentOpen = new TermQueryBuilder(fieldAllowDocument,NOSECURITY_TOKEN);
        QueryBuilder denyDocumentOpen = new TermQueryBuilder(fieldDenyDocument,NOSECURITY_TOKEN);

        if (userAccessTokens == null || userAccessTokens.size() == 0)
        {
            // Only open documents can be included.
            // That query is:
            // (fieldAllowShare is empty AND fieldDenyShare is empty AND fieldAllowDocument is empty AND fieldDenyDocument is empty)
            // We're trying to map to:  -(fieldAllowShare:*) , which should be pretty efficient in Solr because it is negated.  If this turns out not to be so, then we should
            // have the SolrConnector inject a special token into these fields when they otherwise would be empty, and we can trivially match on that token.
            bq.must(allowShareOpen);
            bq.must(denyShareOpen);
            bq.must(allowParentOpen);
            bq.must(denyParentOpen);
            bq.must(allowDocumentOpen);
            bq.must(denyDocumentOpen);
        }
        else
        {
            // Extend the query appropriately for each user access token.
            bq.must(calculateCompleteSubquery(fieldAllowShare,fieldDenyShare,allowShareOpen,denyShareOpen,userAccessTokens));
            bq.must(calculateCompleteSubquery(fieldAllowDocument,fieldDenyDocument,allowDocumentOpen,denyDocumentOpen,userAccessTokens));
            bq.must(calculateCompleteSubquery(fieldAllowParent,fieldDenyParent,allowParentOpen,denyParentOpen,userAccessTokens));
        }

        return bq;
    }


    /** Calculate a complete subclause, representing something like:
     * ((fieldAllowShare is empty AND fieldDenyShare is empty) OR fieldAllowShare HAS token1 OR fieldAllowShare HAS token2 ...)
     *     AND fieldDenyShare DOESN'T_HAVE token1 AND fieldDenyShare DOESN'T_HAVE token2 ...
     */
    protected static QueryBuilder calculateCompleteSubquery(String allowField, String denyField, QueryBuilder allowOpen, QueryBuilder denyOpen, List<String> userAccessTokens)
    {
        BoolQueryBuilder bq = new BoolQueryBuilder();
        // No ES equivalent - hope this is done right inside
        //bq.setMaxClauseCount(1000000);

        // Add the empty-acl case
        BoolQueryBuilder subUnprotectedClause = new BoolQueryBuilder();
        subUnprotectedClause.must(allowOpen);
        subUnprotectedClause.must(denyOpen);
        bq.should(subUnprotectedClause);
        for (String accessToken : userAccessTokens)
        {
            bq.should(new TermQueryBuilder(allowField,accessToken));
            bq.mustNot(new TermQueryBuilder(denyField,accessToken));
        }
        return bq;
    }

}
