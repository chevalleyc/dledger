package com.myvpacare.ledgerservice;

import com.ethercis.ehr.building.I_ContentBuilder;
import com.ethercis.ehr.knowledge.I_KnowledgeCache;
import org.openehr.build.SystemValue;
import org.openehr.rm.common.generic.PartyIdentified;
import org.openehr.rm.composition.Composition;
import org.openehr.rm.composition.EventContext;
import org.openehr.rm.datatypes.text.CodePhrase;

import java.util.HashMap;
import java.util.Map;

public class DummyComposition {

    private final I_KnowledgeCache knowledge;

    public DummyComposition(I_KnowledgeCache knowledge) {
        this.knowledge = knowledge;
    }

    public Composition createDummyComposition(String templateId) throws Exception {
        I_ContentBuilder contentBuilder = I_ContentBuilder.getInstance(knowledge, templateId);
        return contentBuilder.generateNewComposition();
    }

    public Composition createDummyCompositionWithParameters(String templateId,
                                         PartyIdentified composer,
                                         CodePhrase language,
                                         CodePhrase encoding,
                                         CodePhrase territory,
                                         EventContext context) throws Exception {

        Map<SystemValue, Object> values = new HashMap<>();

        values.put(SystemValue.COMPOSER, composer);
        values.put(SystemValue.LANGUAGE, language);
        values.put(SystemValue.ENCODING, encoding);
        values.put(SystemValue.TERRITORY, territory);
        values.put(SystemValue.CONTEXT, context);

        I_ContentBuilder contentBuilder = I_ContentBuilder.getInstance(values, knowledge, templateId);
        return contentBuilder.generateNewComposition();
    }
}
