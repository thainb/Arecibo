package com.ning.arecibo.alertmanager.utils.columns;

import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;

public class PropertyColumnWithCssClass<T> extends PropertyColumn<T> {

    private final String cssClass;

    public PropertyColumnWithCssClass(IModel<String> displayModel,String sortProperty,String propertyExpression,String cssClass) {
        super(displayModel,sortProperty,propertyExpression);
        this.cssClass = cssClass;
    }

    @Override
    public String getCssClass() {
        
        if(this.cssClass == null || this.cssClass.length() == 0)
            return super.getCssClass();

        return this.cssClass;
    }
}
