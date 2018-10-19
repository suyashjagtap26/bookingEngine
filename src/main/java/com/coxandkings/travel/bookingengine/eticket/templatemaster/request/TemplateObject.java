
package com.coxandkings.travel.bookingengine.eticket.templatemaster.request;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"cnk.documenttemplatemanagement.DocumentTemplateManagement"})
public class TemplateObject {

  @JsonProperty("cnk.documenttemplatemanagement.DocumentTemplateManagement")
  private DocumentTemplateManagement documentTemplateManagement;

  public TemplateObject(TemplateInfo templateInfo, List<DynamicAttributes> attributes,
      String userId, String transactionId) {
    this.documentTemplateManagement =
        new DocumentTemplateManagement(templateInfo, attributes, userId, transactionId);
  }

  @JsonProperty("cnk.documenttemplatemanagement.DocumentTemplateManagement")
  public DocumentTemplateManagement getDocumentTemplateManagement() {
    return documentTemplateManagement;
  }

  @JsonProperty("cnk.documenttemplatemanagement.DocumentTemplateManagement")
  public void setDocumentTemplateManagement(DocumentTemplateManagement documentTemplateManagement) {
    this.documentTemplateManagement = documentTemplateManagement;
  }
}
