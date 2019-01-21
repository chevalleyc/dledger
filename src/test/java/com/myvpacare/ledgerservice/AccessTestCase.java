package com.myvpacare.ledgerservice;

import com.ethercis.dao.access.interfaces.I_DomainAccess;
import com.ethercis.dao.access.support.DummyDataAccess;
import com.ethercis.ehr.knowledge.I_KnowledgeCache;
import com.ethercis.ehr.knowledge.KnowledgeCache;
import com.ethercis.opt.query.I_IntrospectCache;
import com.ethercis.persistence.RoleControlledSession;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.service.test.TestServiceBase;
import junit.framework.TestCase;
import org.jooq.DSLContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * to make my life easier...
 */
public class AccessTestCase extends TestServiceBase {

    protected I_DomainAccess testDomainAccess;
    protected DSLContext context;
    protected I_KnowledgeCache knowledge;
    protected I_IntrospectCache introspectCache;

    protected void setupDomainAccess() throws Exception {
        Properties props = new Properties();
        props.put("knowledge.path.archetype", "src/test/resources/knowledge/empty");
        props.put("knowledge.path.template", "src/test/resources/knowledge/empty");
        props.put("knowledge.path.opt", "src/test/resources/knowledge/operational_templates");
        props.put("knowledge.cachelocatable", "true");
        props.put("knowledge.forcecache", "true");

        knowledge = new KnowledgeCache(null, props);

        Pattern include = Pattern.compile(".*");

        knowledge.retrieveFileMap(include, null);

        Map<String, Object> properties = new HashMap<>();
        properties.put(I_DomainAccess.KEY_DIALECT, "POSTGRES");
        properties.put(I_DomainAccess.KEY_URL, "jdbc:postgresql://" + System.getProperty("test.db.host") + ":" + System.getProperty("test.db.port") + "/" + System.getProperty("test.db.name"));
        properties.put(I_DomainAccess.KEY_LOGIN, System.getProperty("test.db.user"));
        properties.put(I_DomainAccess.KEY_PASSWORD, System.getProperty("test.db.password"));

        properties.put(I_DomainAccess.KEY_KNOWLEDGE, knowledge);

        try {
            testDomainAccess = new DummyDataAccess(properties);
        } catch (Exception e) {
            e.printStackTrace();
        }

        context = testDomainAccess.getContext();
        introspectCache = testDomainAccess.getIntrospectCache().load().synchronize();
    }

}