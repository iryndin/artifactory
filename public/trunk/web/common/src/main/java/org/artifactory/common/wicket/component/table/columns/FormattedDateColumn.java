/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.common.wicket.component.table.columns;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.api.config.CentralConfigService;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Extension of the property column meant for date columns to be formatted according to the pattern that is defined
 * in the artifactory.config.xml
 *
 * @author Tomer Cohen
 * @see org.artifactory.api.config.CentralConfigService#getDateFormatter()
 */
public class FormattedDateColumn extends PropertyColumn {
    private CentralConfigService centralConfigService;
    private String originalFormat;
    private String targetFormat;


    public FormattedDateColumn(IModel displayModel, String sortProperty, String propertyExpression,
            CentralConfigService centralConfigService, String originalFormat) {
        super(displayModel, sortProperty, propertyExpression);
        this.centralConfigService = centralConfigService;
        this.originalFormat = originalFormat;
    }

    public FormattedDateColumn(IModel displayModel, String sortProperty, String propertyExpression,
            CentralConfigService centralConfigService, String originalFormat, String targetFormat) {
        super(displayModel, sortProperty, propertyExpression);
        this.centralConfigService = centralConfigService;
        this.originalFormat = originalFormat;
        this.targetFormat = targetFormat;
    }

    public FormattedDateColumn(IModel displayModel, String propertyExpression, String targetFormat) {
        super(displayModel, propertyExpression);
        this.targetFormat = targetFormat;
    }

    @Override
    protected IModel createLabelModel(IModel embeddedModel) {
        IModel model = super.createLabelModel(embeddedModel);
        String dateAsString = (String) model.getObject();
        DateFormat formatter;
        if (StringUtils.isNotBlank(targetFormat)) {
            formatter = new SimpleDateFormat(targetFormat);
        } else {
            formatter = centralConfigService.getDateFormatter();
        }
        DateFormat simpleDateFormat;
        if (StringUtils.isNotBlank(originalFormat)) {
            simpleDateFormat = new SimpleDateFormat(originalFormat);
        } else {
            simpleDateFormat = centralConfigService.getDateFormatter();
        }
        try {
            return new Model(formatter.format(simpleDateFormat.parse(dateAsString)));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return model;
    }
}
