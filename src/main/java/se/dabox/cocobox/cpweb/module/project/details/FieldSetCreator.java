/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.details;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.coursedesign.ComponentConstants;
import se.dabox.service.common.coursedesign.ComponentFieldName;
import se.dabox.service.common.coursedesign.ValidateDesignDataResponse;
import se.dabox.service.common.coursedesign.reldate.RelativeDateCalculator;
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
    private static final Set<String> EPOCH_MANDATORY_FIELDS = new HashSet<>(
            Arrays.asList("starts", "ends"));

    private final CourseDesignDefinition cdd;
    private final OrgProject project;

    public FieldSetCreator(CourseDesignDefinition cdd, OrgProject project) {
        this.cdd = cdd;
        this.project = project;
    }

    public Map<String, Set<ExtendedComponentFieldName>> createFieldMapSet(ValidateDesignDataResponse resp) {

        List<ExtendedComponentFieldName> fields = enhanceFields(resp.getFields());

        return CollectionsUtil.createMapSet(fields,
                new Transformer<ExtendedComponentFieldName, String>() {
                    @Override
                    public String transform(ExtendedComponentFieldName obj) {
                        return obj.getCid().toString();
                    }
                }, LinkedHashSetFactory.<ExtendedComponentFieldName>getInstance());
    }

    private List<ExtendedComponentFieldName> enhanceFields(List<ComponentFieldName> fields) {
        final Map<UUID,Component> compMap = cdd.getComponentMap();

        final Set<String> primaryTypes = createPrimaryTypes();

        return CollectionsUtil.transformList(fields, new Transformer<ComponentFieldName, ExtendedComponentFieldName>() {
            private final RelativeDateCalculator calc = new RelativeDateCalculator().loadCourse(cdd);

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
        });
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

}
