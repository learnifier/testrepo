/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.details;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.service.common.coursedesign.ComponentConstants;
import se.dabox.service.common.coursedesign.ComponentFieldName;
import se.dabox.service.common.coursedesign.ValidateDesignDataResponse;
import se.dabox.service.common.coursedesign.reldate.RelativeDateCalculator;
import se.dabox.service.common.coursedesign.reldate.RelativeDateErrorException;
import se.dabox.service.common.coursedesign.v1.Component;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.coursedesign.validator.impl.TypeHandler;
import se.dabox.service.coursedesign.validator.spi.CourseComponentValidator;
import se.dabox.service.coursedesign.validator.spi.DataField;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.LinkedHashSetFactory;
import se.dabox.util.collections.Transformer;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class FieldSetCreator {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(FieldSetCreator.class);
    
    private final CourseDesignDefinition cdd;

    public FieldSetCreator(CourseDesignDefinition cdd) {
        this.cdd = cdd;
    }

    public Map<String, Set<ExtendedComponentFieldName>> createFieldMapSet(ValidateDesignDataResponse resp) {

        List<ExtendedComponentFieldName> fields = enhanceFields(resp.getFields());

        return CollectionsUtil.createMapSet(fields, (ExtendedComponentFieldName obj) ->
                obj.getCid().toString(), LinkedHashSetFactory.<ExtendedComponentFieldName>getInstance());
    }

    private List<ExtendedComponentFieldName> enhanceFields(List<ComponentFieldName> fields) {
        final Map<UUID,Component> compMap = cdd.getComponentMap();

        final Set<String> primaryTypes = createPrimaryTypes();

        return CollectionsUtil.transformList(fields, new ComponentFieldEnhancer(compMap, primaryTypes));
    }

    private Set<String> createPrimaryTypes() {
        final Set<String> set;
        set = new HashSet<>();

        for (CourseComponentValidator courseComponentValidator : TypeHandler.getValidators()) {
            for (DataField dataField : courseComponentValidator.getFields(null, null)) {
                if (dataField.isRequired()) {
                    set.add(courseComponentValidator.getComponentType());
                    break;
                }
            }
        }

        return set;
    }

    private class ComponentFieldEnhancer implements Transformer<ComponentFieldName, ExtendedComponentFieldName> {

        private final Map<UUID, Component> compMap;
        private final Set<String> primaryTypes;

        private final RelativeDateCalculator calc = new RelativeDateCalculator();

        public ComponentFieldEnhancer(Map<UUID, Component> compMap, Set<String> primaryTypes) {
            this.compMap = compMap;
            this.primaryTypes = primaryTypes;
            try {
                calc.loadCourse(cdd);
            } catch (RelativeDateErrorException ex){
                LOGGER.warn("Relative date calculation errors: {}", ex.getErrors());
            }
        }



        @Override
        public ExtendedComponentFieldName transform(ComponentFieldName item) {
            ExtendedComponentFieldName retval = new ExtendedComponentFieldName(item.getCid(),
                    item.getName(), item.getDefaultValue(), item.getDataType(), item.
                            isMandatory());
            retval.setComponent(compMap.get(item.getCid()));

            retval.setPrimaryField(isPrimaryField(retval));

            return retval;
        }

        private boolean isPrimaryField(ExtendedComponentFieldName extendedComponentField) {
            if (calc.isEpoch(extendedComponentField.getComponent())) {
                return true;
            } else if (isNonRelativeConditionalTimeComponent(extendedComponentField)) {
                return true;
            } else if (calc.isRelative(extendedComponentField.getComponent())) {
                return false;
            } else if (primaryTypes.contains(extendedComponentField.getComponent().getBasetype())) {
                return true;
            }

            return false;
        }

        private boolean isNonRelativeConditionalTimeComponent(
                ExtendedComponentFieldName extendedComponentField) {
            if (!extendedComponentField.getName().equals(ComponentConstants.FIELD_ENABLEDATE)) {
                return false;
            }

            String dateRule = extendedComponentField.getComponent().getProperties().get(
                    ComponentConstants.PROP_RELATIVEENABLE);

            return StringUtils.isBlank(dateRule);
        }
    }

}
